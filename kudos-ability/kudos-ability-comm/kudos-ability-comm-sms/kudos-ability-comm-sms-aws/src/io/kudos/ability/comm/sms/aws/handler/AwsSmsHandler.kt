package io.kudos.ability.comm.sms.aws.handler

import io.kudos.ability.comm.sms.aws.init.properties.SmsAwsProxyProperties
import io.kudos.ability.comm.sms.aws.model.AwsSmsCallBackParam
import io.kudos.ability.comm.sms.aws.model.AwsSmsRequest
import io.kudos.base.logger.LogFactory
import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.exception.SdkServiceException
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.apache.ProxyConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import java.net.URI

/**
 * AWS短信发送处理器（支持可配置 endpoint，便于用 WireMock/Testcontainers 做离线测试）
 *
 * @author paul
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
class AwsSmsHandler {

    /** 代理配置，由 Spring 注入；启用时影响进程级共享的 [HTTP_CLIENT] */
    @Resource
    private lateinit var proxyProperties: SmsAwsProxyProperties

    /** 可选：覆盖 SNS 终端。生产留空；测试注入 http://host:port */
    @Value($$"${kudos.ability.comm.sms.aws.endpoint}")
    private lateinit var endpointOverride: String

    /**
     * AWS 发送短信（无回调）。等价于 `send(smsRequest, null)`，用于"发完不关心结果"的场景。
     *
     * @param smsRequest 短信请求（含 region/credential/手机号/正文）
     * @author K
     * @since 1.0.0
     */
    fun send(smsRequest: AwsSmsRequest) {
        send(smsRequest, null)
    }

    /**
     * AWS 发送短信（异步，虚拟线程），完成后回调。
     *
     * 使用 `Thread.ofVirtual()` 而非 `CoroutineScope.launch` 是因为本模块下游可能尚未引入协程；
     * 同时虚拟线程在阻塞 IO 上几乎零开销，对短信这种"调一次就完"的场景足够。
     *
     * @param smsRequest 短信请求
     * @param callback 完成回调；不管成功失败都会被调用一次（含降级的 `599 client error`）
     * @author K
     * @since 1.0.0
     */
    fun send(smsRequest: AwsSmsRequest, callback: ((AwsSmsCallBackParam) -> Unit)? = null) {
        Thread.ofVirtual().start { doSend(smsRequest, callback) }
    }

    /**
     * 实际发送逻辑（在虚拟线程上执行）。
     *
     * 流程：构建带代理/可覆盖 endpoint 的 [SnsClient] → 组装 [PublishRequest]（messageAttributes 过滤掉 null kv）
     *      → 调用 publish 并把 sdk 响应封装成 [AwsSmsCallBackParam] → finally 关闭 client 并兜底回调。
     *
     * 异常分三类各自归并状态码：
     * - [AwsServiceException]：AWS 业务异常（如鉴权失败、限流），取 `statusCode`/`errorMessage`
     * - [SdkServiceException]：底层 SDK 服务异常（非 2xx），同上
     * - 其它：本地错误（DNS 解析、序列化）—— 不带 statusCode，仅日志
     *
     * 任何路径都保证 callback 至少被调用一次，避免上层悬挂。
     *
     * @param smsRequest 短信请求
     * @param callback 可选回调
     * @author K
     * @since 1.0.0
     */
    private fun doSend(
        smsRequest: AwsSmsRequest,
        callback: ((AwsSmsCallBackParam) -> Unit)? = null
    ) {
        var snsClient: SnsClient? = null
        var cb: AwsSmsCallBackParam? = null

        try {
            // 1) 构建 SnsClient（支持代理 + endpoint 覆盖）
            val credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(smsRequest.accessKeyId, smsRequest.accessKeySecret)
            )

            val builder = SnsClient.builder()
                .region(Region.of(smsRequest.region))
                .credentialsProvider(credentialsProvider)

            if (HTTP_CLIENT != null) builder.httpClient(HTTP_CLIENT)
            if (endpointOverride.isNotBlank()) {
                // 测试时注入，例如 "http://localhost:1080"
                builder.endpointOverride(URI.create(endpointOverride.trim()))
            }

            snsClient = builder.build()

            // 2) 构建 PublishRequest
            val reqBuilder = PublishRequest.builder()
                .phoneNumber(smsRequest.phoneNumber)
                .message(smsRequest.message)

            smsRequest.messageAttributes
                ?.mapNotNull { (k, v) -> if (k != null && v != null) k to v else null }
                ?.toMap()
                ?.takeIf { it.isNotEmpty() }
                ?.let { reqBuilder.messageAttributes(it) }

            val request = reqBuilder.build()

            // 3) 发送并解析响应
            LOG.info("[aws] 开始发送短信...")
            val result = snsClient.publish(request)
            val http = result.sdkHttpResponse()

            cb = AwsSmsCallBackParam().apply {
                messageId = result.messageId()
                sequenceNumber = result.sequenceNumber()
                statusCode = http.statusCode()
                statusText = http.statusText().orElse("OK")
            }
            LOG.info("[aws] 发送短信成功, 结果:{0}", cb)
        } catch (e: AwsServiceException) {
            cb = AwsSmsCallBackParam().apply {
                statusCode = e.statusCode()
                statusText = e.awsErrorDetails()?.errorMessage()
                    ?: e.awsErrorDetails()?.errorCode()
                    ?: "AwsServiceException"
            }
            LOG.error(e, "[aws] 发送短信失败（AWS 服务异常）")
        } catch (e: SdkServiceException) {
            cb = AwsSmsCallBackParam().apply {
                statusCode = e.statusCode()
                statusText = e.message ?: "ServiceException"
            }
            LOG.error(e, "[aws] 发送短信失败（非 2xx 响应）")
        } catch (e: Exception) {
            // 其它本地错误（连接失败、序列化错误等）
            LOG.error(e, "[aws] 发送短信失败（本地异常）")
        } finally {
            try {
                snsClient?.close()
            } catch (_: Exception) {
            }

            // 安全回调：无论上面哪条分支，保证一定回调一个对象
            val safe = cb ?: AwsSmsCallBackParam().apply {
                statusCode = 599
                statusText = "client error"
            }
            callback?.invoke(safe)
        }
    }

    /**
     * `@PostConstruct` 初始化：若代理启用则构建一个进程级 [ApacheHttpClient] 复用。
     * 不启用代理时不构造 client——让 SDK 走默认 client，避免无端引入额外的连接池。
     *
     * @author K
     * @since 1.0.0
     */
    @PostConstruct
    private fun initApacheHttpClient() {
        if (proxyProperties.enable) {
            LOG.info("aws短信发送，启用 HTTP 代理")
            HTTP_CLIENT = ApacheHttpClient.builder()
                .proxyConfiguration(
                    ProxyConfiguration.builder()
                        .endpoint(URI.create(requireNotNull(proxyProperties.url) { "proxy url is null" }))
                        .username(proxyProperties.username)
                        .password(proxyProperties.password)
                        .build()
                )
                .build()
        }
    }

    /**
     * 全进程共享的 `SdkHttpClient`（启用代理时由 `initApacheHttpClient` 赋值）。
     * 代理配置变更需重启进程；多租户若需不同代理应另行设计客户端工厂。
     */
    companion object {
        /** 日志器 */
        private val LOG = LogFactory.getLog(this::class)
        /** 进程级共享的 SDK HTTP 客户端；仅在代理启用时由 [initApacheHttpClient] 赋值 */
        private var HTTP_CLIENT: SdkHttpClient? = null
    }
}
