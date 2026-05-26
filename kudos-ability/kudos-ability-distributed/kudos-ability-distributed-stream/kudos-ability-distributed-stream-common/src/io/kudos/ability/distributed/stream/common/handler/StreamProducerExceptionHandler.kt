package io.kudos.ability.distributed.stream.common.handler

import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.ability.distributed.stream.common.model.vo.StreamProducerMsgVo
import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.base.data.json.JsonKit
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.retry.AbstractFailedDataHandler
import io.kudos.context.retry.RetryConfig
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.GenericMessage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.full.primaryConstructor

/**
 * Stream message producer exception handler.
 *
 * Handles message send failures: persists failed messages to local files and retries
 * sending them on a schedule.
 *
 * Core features:
 * 1. Failed message persistence: saves failed messages to the local file system to
 *    avoid message loss.
 * 2. Scheduled retry: a scheduled task (default once per minute) scans the failed
 *    message files and re-sends them.
 * 3. Message recovery: reconstructs the message object (body and headers) from the
 *    persisted JSON data.
 * 4. Retry flag: marks isResend=true during retry sends to avoid re-triggering the
 *    failure handling.
 *
 * Workflow:
 * - On send failure, the failed message is serialized to JSON and saved to a local file.
 * - The scheduled task scans the failed message files and deserializes the messages.
 * - The Message object (body and headers) is rebuilt.
 * - StreamProducerHelper.doRealSend is called to retry the send.
 * - On success, the local file is deleted; on failure, the file is kept for the next retry.
 *
 * File storage:
 * - File path: {configured path}/{service code}/, supports per-service isolation.
 * - File format: JSON, containing bindName, msgHeaderJson and msgBodyJson.
 *
 * Notes:
 * - If the retry send still fails, the message remains in the local file.
 * - An exception-data repair mechanism is needed to prevent failed messages piling up
 *   and causing disk-space issues.
 * - The scheduled task interval is configurable via cronExpression.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class StreamProducerExceptionHandler : AbstractFailedDataHandler<StreamProducerMsgVo>(), IStreamFailHandler {

    /**
     * Root directory for failed message persistence. Priority:
     * 1. Spring property `kudos.ability.distributed.stream.produce-fail-path` (injected via @Value).
     * 2. [RetryConfig.baseFailedDataPath] (system property / environment variable /
     *    `${java.io.tmpdir}/kudos-failed-data`).
     *
     * The legacy default was hard-coded to `/var/data/failed` — Windows / volumeless
     * containers crashed outright, so it has been replaced.
     */
    @Value($$"${kudos.ability.distributed.stream.produce-fail-path:}")
    private var configuredFilePath: String = ""

    @Resource
    private lateinit var streamProducerHelper: StreamProducerHelper

    /**
     * Processes failed message data.
     *
     * Reconstructs the message object from persisted JSON data and re-sends it to MQ.
     *
     * Workflow:
     * 1. Extract message info: pull bindName, msgBodyJson and msgHeaderJson from
     *    StreamProducerMsgVo.
     * 2. Deserialize the body: convert msgBodyJson into the business object.
     * 3. Build StreamMessageVo: wrap the business object as a StreamMessageVo.
     * 4. Deserialize the headers: convert msgHeaderJson into a Map and build
     *    MessageHeaders.
     * 5. Build the Message object: wrap the body and headers with GenericMessage.
     * 6. Retry send: call StreamProducerHelper.doRealSend with isResend=true.
     *
     * Retry flag:
     * - isResend=true indicates a retry send and avoids re-triggering the failure
     *   handling mechanism.
     * - If the retry send fails, the message remains in the local file for the next
     *   retry.
     *
     * Return value:
     * - true: send operation succeeded (note: MQ send is asynchronous, so true does
     *   not guarantee the message reached the server).
     * - false: send failed; the message remains in the local file.
     *
     * Notes:
     * - If the send raises an asynchronous flush error, it will continue to be
     *   intercepted by the exception handling mechanism.
     * - If false is returned directly, the message was not submitted to the MQ
     *   pending-flush queue and the local file will not be deleted.
     * - Ensure the JSON data is well-formed, otherwise deserialization will fail.
     *
     * @param data the failed message data object, containing bindName, msgBodyJson and msgHeaderJson
     * @return true if the send operation succeeded (asynchronously), false on failure
     */
    override fun processFailedData(data: StreamProducerMsgVo): Boolean {
        val bindName = requireNotNull(data.bindName) { "bindName must not be null" }
        val msgBodyJson = requireNotNull(data.msgBodyJson) { "msgBodyJson must not be null" }
        val msgHeaderJson = requireNotNull(data.msgHeaderJson) { "msgHeaderJson must not be null" }
        val obj = requireNotNull(readMessageBody(msgBodyJson, data.msgBodyClassName)) { "Failed to deserialize msgBodyJson" }
        val streamMessageVo: StreamMessageVo<Any?> = StreamMessageVo(obj)
        val headMap = requireNotNull(JsonKit.fromJson<MutableMap<String, Any>>(msgHeaderJson)) { "Failed to deserialize msgHeaderJson" }
        val messageHeaders = MessageHeaders(headMap)
        val message = GenericMessage(streamMessageVo, messageHeaders)
        // If this send raises an asynchronous flush error, it will continue to be intercepted as an exception.
        // If this send returns false directly, it was not submitted to the MQ pending-flush queue and the local file will not be deleted.
        return streamProducerHelper.doRealSend(bindName, message, true)
    }

    private fun readMessageBody(msgBodyJson: String, className: String?): Any? {
        if (!className.isNullOrBlank()) {
            runCatching {
                @Suppress("UNCHECKED_CAST")
                val kClass = Class.forName(className).kotlin as KClass<Any>
                return JsonKit.readValue(msgBodyJson.toByteArray(Charsets.UTF_8), kClass)
            }
            runCatching {
                @Suppress("UNCHECKED_CAST")
                val kClass = Class.forName(className).kotlin as KClass<Any>
                val dynamicBody = readDynamicJson(msgBodyJson)
                return restoreByPrimaryConstructor(dynamicBody, kClass)
            }
        }
        return readDynamicJson(msgBodyJson)
    }

    private fun restoreByPrimaryConstructor(dynamicBody: Any?, kClass: KClass<Any>): Any? {
        val values = dynamicBody as? Map<*, *> ?: return null
        val constructor = kClass.primaryConstructor ?: return null
        val args = constructor.parameters
            .filter { values.containsKey(it.name) }
            .associateWith { parameter ->
                coerceConstructorValue(values[parameter.name], parameter.type.classifier)
            }
        return constructor.callBy(args)
    }

    private fun coerceConstructorValue(value: Any?, classifier: KClassifier?): Any? =
        when (classifier) {
            Int::class -> (value as? Number)?.toInt()
            Long::class -> (value as? Number)?.toLong()
            Double::class -> (value as? Number)?.toDouble()
            Float::class -> (value as? Number)?.toFloat()
            Short::class -> (value as? Number)?.toShort()
            Byte::class -> (value as? Number)?.toByte()
            Boolean::class -> value as? Boolean
            String::class -> value?.toString()
            else -> value
        }

    private fun readDynamicJson(msgBodyJson: String): Any? {
        val element = JsonKit.defaultJson.parseToJsonElement(msgBodyJson)
        return unwrapJsonElement(element)
    }

    private fun unwrapJsonElement(element: JsonElement): Any? = when (element) {
        is JsonObject -> element.mapValues { unwrapJsonElement(it.value) }
        is JsonArray -> element.map { unwrapJsonElement(it) }
        is JsonPrimitive -> element.booleanOrNull
            ?: element.intOrNull
            ?: element.longOrNull
            ?: element.doubleOrNull
            ?: element.content
        JsonNull -> null
    }

    override val businessType: String
        get() = "mq-producer-fail"

    override val cronExpression: String
        get() = "0 0/1 * * * *"

    override fun filePath(): String {
        val basePath = configuredFilePath.takeIf { it.isNotBlank() } ?: RetryConfig.baseFailedDataPath
        val serviceCode = KudosContextHolder.getOrNull()?.atomicServiceCode ?: "default"
        return "$basePath/$serviceCode"
    }

    override fun bindName(): String = IStreamFailHandler.DEFAULT_BIND_NAME

}
