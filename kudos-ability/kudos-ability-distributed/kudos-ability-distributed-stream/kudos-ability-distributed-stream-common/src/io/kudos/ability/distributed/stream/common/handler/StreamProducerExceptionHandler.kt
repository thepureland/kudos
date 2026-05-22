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
 * 流式消息生产者异常处理器
 * 
 * 处理消息发送失败的情况，支持将失败消息持久化到本地文件，并定时重试发送。
 * 
 * 核心功能：
 * 1. 失败消息持久化：将发送失败的消息保存到本地文件系统，避免消息丢失
 * 2. 定时重试：通过定时任务（默认每分钟执行一次）扫描失败消息文件，重新发送
 * 3. 消息恢复：从持久化的JSON数据中恢复消息对象，包括消息体和消息头
 * 4. 重试标识：重试发送时标记isResend=true，避免重复触发失败处理
 * 
 * 工作流程：
 * - 消息发送失败时，失败消息会被序列化为JSON格式保存到本地文件
 * - 定时任务扫描失败消息文件，反序列化消息对象
 * - 重新构建Message对象（包括消息体和消息头）
 * - 调用StreamProducerHelper.doRealSend进行重试发送
 * - 发送成功后删除本地文件，失败则保留文件等待下次重试
 * 
 * 文件存储：
 * - 文件路径：{配置路径}/{服务代码}/，支持按服务隔离
 * - 文件格式：JSON格式，包含bindName、msgHeaderJson、msgBodyJson
 * 
 * 注意事项：
 * - 如果重试发送仍然失败，消息会继续保留在本地文件中
 * - 需要提供异常数据修复机制，避免失败消息堆积导致磁盘空间问题
 * - 定时任务执行频率可通过cronExpression配置调整
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class StreamProducerExceptionHandler : AbstractFailedDataHandler<StreamProducerMsgVo>(), IStreamFailHandler {

    /**
     * 失败消息持久化根目录。优先级：
     * 1. Spring 属性 `kudos.ability.distributed.stream.produce-fail-path`（Value 注入）
     * 2. [RetryConfig.baseFailedDataPath]（系统属性 / 环境变量 / `${java.io.tmpdir}/kudos-failed-data`）
     *
     * 历史默认是硬编码的 `/var/data/failed`——Windows / 无 volume 容器直接挂掉，已替换。
     */
    @Value($$"${kudos.ability.distributed.stream.produce-fail-path:}")
    private var configuredFilePath: String = ""

    @Resource
    private lateinit var streamProducerHelper: StreamProducerHelper

    /**
     * 处理失败的消息数据
     * 
     * 从持久化的JSON数据中恢复消息对象，重新发送到MQ。
     * 
     * 工作流程：
     * 1. 提取消息信息：从StreamProducerMsgVo中提取绑定名称、消息体JSON、消息头JSON
     * 2. 反序列化消息体：将msgBodyJson反序列化为业务对象
     * 3. 创建StreamMessageVo：包装业务对象为StreamMessageVo
     * 4. 反序列化消息头：将msgHeaderJson反序列化为Map，创建MessageHeaders
     * 5. 构建Message对象：使用GenericMessage封装消息体和消息头
     * 6. 重试发送：调用StreamProducerHelper.doRealSend进行重试，标记isResend=true
     * 
     * 重试标识：
     * - isResend=true表示这是重试发送，避免重复触发失败处理机制
     * - 如果重试发送失败，消息会继续保留在本地文件中，等待下次重试
     * 
     * 返回值说明：
     * - true：发送操作成功（注意：MQ发送是异步的，true不代表消息已到达服务器）
     * - false：发送失败，消息会继续保留在本地文件中
     * 
     * 注意事项：
     * - 如果本次发送为异步flush报错，会继续被异常拦截机制处理
     * - 如果本次直接返回false，消息没提交到MQ的等待flush队列中，本地文件不会删除
     * - 需要确保JSON数据格式正确，否则反序列化会失败
     * 
     * @param data 失败消息数据对象，包含绑定名称、消息体JSON、消息头JSON
     * @return true表示发送操作成功（异步），false表示发送失败
     */
    override fun processFailedData(data: StreamProducerMsgVo): Boolean {
        val bindName = requireNotNull(data.bindName) { "bindName 不能为空" }
        val msgBodyJson = requireNotNull(data.msgBodyJson) { "msgBodyJson 不能为空" }
        val msgHeaderJson = requireNotNull(data.msgHeaderJson) { "msgHeaderJson 不能为空" }
        val obj = requireNotNull(readMessageBody(msgBodyJson, data.msgBodyClassName)) { "msgBodyJson 反序列化失败" }
        val streamMessageVo: StreamMessageVo<Any?> = StreamMessageVo(obj)
        val headMap = requireNotNull(JsonKit.fromJson<MutableMap<String, Any>>(msgHeaderJson)) { "msgHeaderJson 反序列化失败" }
        val messageHeaders = MessageHeaders(headMap)
        val message = GenericMessage(streamMessageVo, messageHeaders)
        //如果本次发送为异步flush报错，则它会继续被异常拦截
        //如果本次直接返回false，它则没提交到mq的等待flush队列中，本地文件不做删除
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
