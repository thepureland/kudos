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

    @Value("\${kudos.ability.comm.sms.aliyun}")
    private lateinit var endpointOverrideStr: String

    /**
     * 异步发送短信
     * 
     * 使用虚拟线程异步发送短信，完成后通过回调返回结果。
     * 
     * 工作流程：
     * 1. 创建虚拟线程：使用Thread.ofVirtual()创建轻量级虚拟线程
     * 2. 异步执行：在虚拟线程中执行doSend方法
     * 3. 非阻塞：调用线程立即返回，不等待发送完成
     * 
     * 虚拟线程优势：
     * - 轻量级：相比传统线程，虚拟线程资源消耗更少
     * - 高并发：可以创建大量虚拟线程处理并发请求
     * - 适合IO密集型任务：短信发送是网络IO操作，适合使用虚拟线程
     * 
     * 注意事项：
     * - 发送结果通过回调函数返回，不会阻塞调用线程
     * - 即使发送失败，也会通过回调返回错误信息
     * 
     * @param smsRequest 短信发送请求对象
     * @param callback 发送完成后的回调函数，接收SendSmsResponseBody结果
     */
    fun send(smsRequest: AliyunSmsRequest, callback: (SendSmsResponseBody) -> Unit) {
        Thread.ofVirtual().start { doSend(smsRequest, callback) }
    }

    /**
     * 执行短信发送
     * 
     * 构建阿里云客户端，发送短信请求，并确保资源正确释放。
     * 
     * 工作流程：
     * 1. 构建客户端：根据请求参数构建AsyncClient
     * 2. 构建请求：创建SendSmsRequest对象，设置手机号、签名、模板等
     * 3. 发送请求：调用client.sendSms发送短信
     * 4. 获取结果：等待响应并获取响应体
     * 5. 资源释放：在finally块中关闭客户端
     * 6. 回调通知：无论成功或失败，都通过回调返回结果
     * 
     * 异常处理：
     * - 捕获所有异常，记录错误日志
     * - 即使发生异常，也会构建安全的响应体并回调
     * - 确保客户端资源在finally块中正确释放
     * 
     * 安全响应体：
     * - 如果发送成功，返回实际的响应体
     * - 如果发送失败，构建一个包含错误信息的响应体
     * - 响应体code为"EXCEPTION"，message为异常信息
     * - 确保回调函数始终能收到有效的响应体
     * 
     * 资源管理：
     * - 使用try-finally确保客户端一定会被关闭
     * - 客户端关闭异常会被忽略，不影响主流程
     * 
     * 注意事项：
     * - response.get()是阻塞调用，会等待异步请求完成
     * - 客户端关闭操作在finally块中执行，确保资源释放
     * - 回调函数始终会被调用，即使发生异常
     * 
     * @param smsRequest 短信发送请求对象
     * @param callback 发送完成后的回调函数
     */
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
     * 构建阿里云短信异步客户端
     * 
     * 根据配置构建AsyncClient，支持自定义endpoint（用于测试环境如WireMock）。
     * 
     * 工作流程：
     * 1. 创建凭证提供者：使用AccessKey ID和Secret创建StaticCredentialProvider
     * 2. 构建客户端Builder：设置region和凭证提供者
     * 3. 处理自定义Endpoint（如果配置）：
     *    - 解析endpointOverrideStr（支持多种格式）
     *    - 提取协议（http/https）
     *    - 提取主机和端口
     *    - 创建ClientOverrideConfiguration并应用
     * 4. 构建并返回客户端
     * 
     * Endpoint格式支持：
     * - 完整URI："http://localhost:8080" 或 "https://dysmsapi.aliyuncs.com"
     * - 主机端口："localhost:8080"
     * - 纯域名："dysmsapi.aliyuncs.com"
     * 
     * 协议处理：
     * - 如果URI包含scheme，使用该scheme（转为大写）
     * - 如果URI不包含scheme，默认使用"http"（测试环境）
     * - 生产环境通常使用"https"
     * 
     * 端口处理：
     * - 如果URI包含端口，使用该端口
     * - 如果URI不包含端口（port=-1），只使用主机名
     * - 如果无法解析URI，使用原始字符串（去除空格）
     * 
     * 使用场景：
     * - 生产环境：使用阿里云默认endpoint
     * - 测试环境：使用WireMock等Mock服务
     * - 开发环境：可以使用本地代理
     * 
     * 注意事项：
     * - endpointOverrideStr为空时，使用阿里云默认endpoint
     * - 协议默认为http（适合测试），生产环境建议使用https
     * - URI解析失败时会fallback到原始字符串
     * 
     * @param region 区域代码（如"cn-hangzhou"）
     * @param accessKeyId 阿里云AccessKey ID
     * @param accessKeySecret 阿里云AccessKey Secret
     * @return 配置好的AsyncClient实例
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
