package io.kudos.ability.comm.sms.aliyun.handler

import com.aliyun.auth.credentials.Credential
import com.aliyun.auth.credentials.provider.StaticCredentialProvider
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponseBody
import darabonba.core.client.ClientOverrideConfiguration
import io.kudos.ability.comm.sms.aliyun.model.AliyunSmsRequest
import io.kudos.base.data.json.JsonKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Value
import java.net.URI

/**
 * 阿里云短信发送处理器（支持可配置 endpoint，便于测试注入 WireMock）
 *
 * 配置项：
 * - kudos.sms.aliyun.endpoint（可选）：覆盖默认终端，示例：
 *   - 生产默认留空（SDK 按 region 使用官方域名）
 *   - 测试填 "http://<wiremock-host>:<port>"
 */
class AliyunSmsHandler {

    @Value("\${kudos.sms.aliyun.endpoint:}") // 留空=使用官方端点；测试时注入 WireMock
    private lateinit var endpointOverrideStr: String

    /**
     * 异步发送（虚拟线程），完成后以回调返回结果
     */
    @JvmOverloads
    fun send(smsRequest: AliyunSmsRequest, callback: (SendSmsResponseBody) -> Unit) {
        Thread.ofVirtual().start { doSend(smsRequest, callback) }
    }

    private fun doSend(smsRequest: AliyunSmsRequest, callback: (SendSmsResponseBody) -> Unit) {
        var client: AsyncClient? = null
        var result: SendSmsResponseBody? = null
        var lastError: Exception? = null

        try {
            client = buildClient(
                region = smsRequest.region,
                accessKeyId = smsRequest.accessKeyId,
                accessKeySecret = smsRequest.accessKeySecret
            )

            val request = SendSmsRequest.builder()
                .phoneNumbers(smsRequest.phoneNumbers)
                .signName(smsRequest.signName)
                .templateCode(smsRequest.templateCode)
                .templateParam(smsRequest.templateParam)
                .build()

            LOG.info("[aliyun] 开始发送短信...")
            val response = client.sendSms(request)
            result = response.get()?.body
            LOG.info("[aliyun] 发送短信成功, 结果:{0}", JsonKit.toJson(result ?: "null"))
        } catch (e: Exception) {
            lastError = e
            LOG.error(e, "[aliyun] 发送短信失败")
        } finally {
            try { client?.close() } catch (_: Exception) { /* ignore */ }

            // 始终回调：异常分支返回一个安全的响应体，避免 lateinit 未初始化
            val safe = result ?: SendSmsResponseBody.builder()
                .code("EXCEPTION")
                .message(lastError?.message ?: "unknown error")
                .requestId("local-test")
                .build()

            callback.invoke(safe)
        }
    }

    /**
     * 适配 dysmsapi20170525 v4.0.5 的构建方式：
     * 通过 overrideConfiguration(ClientOverrideConfiguration) 覆盖 endpoint/协议
     */
    private fun buildClient(
        region: String?,
        accessKeyId: String?,
        accessKeySecret: String?
    ): AsyncClient {
        val provider = StaticCredentialProvider.create(
            Credential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .build()
        )

        val builder = AsyncClient.builder()
            .region(region)
            .credentialsProvider(provider)

        // 如果配置了自定义 endpoint（例如 WireMock），则覆盖
        if (endpointOverrideStr.isNotBlank()) {
            // 既兼容 "http://host:port" 也支持 "host:port"/纯域名
            val uri = runCatching { URI(endpointOverrideStr) }.getOrNull()
            val protocol = (uri?.scheme ?: "http").uppercase() // WireMock 用 http；生产默认 https
            val hostPort = when {
                uri == null -> endpointOverrideStr.trim()
                uri.port == -1 -> uri.host
                else -> "${uri.host}:${uri.port}"
            }

            val override = ClientOverrideConfiguration.create()
                .setEndpointOverride(hostPort) // 例："localhost:8080" 或 "dysmsapi.aliyuncs.com"
                .setProtocol(protocol)         // "HTTP" 或 "HTTPS"

            builder.overrideConfiguration(override)
        }

        return builder.build()
    }

    private val LOG = LogFactory.getLog(this)
}
