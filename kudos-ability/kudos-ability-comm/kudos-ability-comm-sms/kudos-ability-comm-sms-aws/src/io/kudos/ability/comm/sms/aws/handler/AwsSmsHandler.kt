package io.kudos.ability.comm.sms.aws.handler

import io.kudos.ability.comm.sms.aws.init.properties.SmsAwsProxyProperties
import io.kudos.ability.comm.sms.aws.model.AwsSmsCallBackParam
import io.kudos.ability.comm.sms.aws.model.AwsSmsRequest
import io.kudos.base.logger.LogFactory
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.apache.ProxyConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import java.net.URI

/**
 * @Description AWS短信发送处理器
 * @Author paul
 * @Date 2023/2/10 16:23
 *
 *
 * 官方文档：https://docs.aws.amazon.com/sns/latest/dg/sns-mobile-phone-number-as-subscriber.html
 */
@Component
class AwsSmsHandler {

    @Autowired
    private lateinit var proxyProperties: SmsAwsProxyProperties

    /**
     * AWS发送短信
     *
     * @param smsRequest
     */
    fun send(smsRequest: AwsSmsRequest) {
        send(smsRequest, null)
    }

    /**
     * AWS发送短信，并处理回调
     *
     * @param smsRequest
     * @param callback
     */
    fun send(smsRequest: AwsSmsRequest, callback: ((AwsSmsCallBackParam) -> Unit)? = null) {
        Thread.ofVirtual().start { doSend(smsRequest, callback) }
    }

    private fun doSend(smsRequest: AwsSmsRequest, callback: ((AwsSmsCallBackParam) -> Unit)? = null) {
        var snsClient: SnsClient? = null
        lateinit var smsCallBackParam: AwsSmsCallBackParam
        try {
            val credentials: AwsCredentials =
                AwsBasicCredentials.create(smsRequest.accessKeyId, smsRequest.accessKeySecret)
            val credentialsProvider = StaticCredentialsProvider.create(credentials)

            val builder = SnsClient.builder()
            if (HTTP_CLIENT != null) {
                builder.httpClient(HTTP_CLIENT)
            }
            snsClient = builder.region(Region.of(smsRequest.region))
                .credentialsProvider(credentialsProvider)
                .build()

            val request = PublishRequest.builder()
                .phoneNumber(smsRequest.phoneNumber)
                .message(smsRequest.message)
                .messageAttributes(smsRequest.messageAttributes)
                .build()

            LOG.info("[aws]开始发送短信...")
            val result = snsClient.publish(request)
            val sdkHttpResponse = result.sdkHttpResponse()

            smsCallBackParam = AwsSmsCallBackParam()
            smsCallBackParam.messageId = result.messageId()
            smsCallBackParam.sequenceNumber = result.sequenceNumber()
            smsCallBackParam.statusCode = sdkHttpResponse.statusCode()
            smsCallBackParam.statusText = sdkHttpResponse.statusText().get()
            LOG.info("[aws]发送短信成功,结果:{0}", smsCallBackParam)
        } catch (e: Exception) {
            LOG.error(e, "[aws]发送短信失败")
        } finally {
            snsClient?.close()
            //执行回调
            callback?.invoke(smsCallBackParam)
        }
    }

    @PostConstruct
    private fun initApacheHttpClient() {
        if (proxyProperties.isEnable) {
            LOG.info("asw短信發送，開啓proxy代理.")
            HTTP_CLIENT = ApacheHttpClient.builder()
                .proxyConfiguration(
                    ProxyConfiguration.builder()
                        .endpoint(URI.create(proxyProperties.url!!))
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
