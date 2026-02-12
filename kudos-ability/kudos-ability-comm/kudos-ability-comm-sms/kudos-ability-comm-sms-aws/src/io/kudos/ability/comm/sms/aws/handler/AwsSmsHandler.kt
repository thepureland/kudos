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

    @Resource
    private lateinit var proxyProperties: SmsAwsProxyProperties

    /** 可选：覆盖 SNS 终端。生产留空；测试注入 http://host:port */
    @Value($$"${kudos.ability.comm.sms.aws.endpoint}")
    private lateinit var endpointOverride: String

    /**
     * AWS 发送短信（无回调）
     */
    fun send(smsRequest: AwsSmsRequest) {
        send(smsRequest, null)
    }

    /**
     * AWS 发送短信（异步，虚拟线程），完成后回调
     */
    fun send(smsRequest: AwsSmsRequest, callback: ((AwsSmsCallBackParam) -> Unit)? = null) {
        Thread.ofVirtual().start { doSend(smsRequest, callback) }
    }

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

            // messageAttributes: MutableMap<String?, MessageAttributeValue?>?
            smsRequest.messageAttributes
                ?.filterKeys { it != null }
                ?.filterValues { it != null }
                ?.mapKeys { requireNotNull(it.key) { "key is null" } }
                ?.mapValues { requireNotNull(it.value) { "value is null" } }
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

    companion object {
        private val LOG = LogFactory.getLog(this)
        private var HTTP_CLIENT: SdkHttpClient? = null
    }
}
