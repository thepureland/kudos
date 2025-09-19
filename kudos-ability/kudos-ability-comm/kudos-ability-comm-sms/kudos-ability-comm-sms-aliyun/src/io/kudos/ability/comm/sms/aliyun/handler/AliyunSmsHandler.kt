package io.kudos.ability.comm.sms.aliyun.handler

import com.aliyun.auth.credentials.Credential
import com.aliyun.auth.credentials.provider.StaticCredentialProvider
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponseBody
import io.kudos.ability.comm.sms.aliyun.model.AliyunSmsRequest
import io.kudos.base.data.json.JsonKit
import io.kudos.base.logger.LogFactory
import org.springframework.stereotype.Component

/**
 * @Description 阿里云短信发送处理器
 * @Author paul
 * @Date 2023/2/10 17:14
 *
 *
 * 官方文档：https://help.aliyun.com/document_detail/419273.html
 */
@Component
class AliyunSmsHandler {
    /**
     * 阿里云发送短信，并处理回调
     *
     * @param smsRequest
     * @param callback
     */
    /**
     * 阿里云发送短信
     *
     * @param smsRequest
     */
    @JvmOverloads
    fun send(smsRequest: AliyunSmsRequest, callback: (SendSmsResponseBody) -> Unit) {
        Thread.ofVirtual().start { doSend(smsRequest, callback) }
    }

    private fun doSend(smsRequest: AliyunSmsRequest, callback: (SendSmsResponseBody) -> Unit) {
        var client: AsyncClient? = null
        lateinit var smsResponse: SendSmsResponseBody
        try {
            val provider = StaticCredentialProvider.create(
                Credential.builder()
                    .accessKeyId(smsRequest.accessKeyId)
                    .accessKeySecret(smsRequest.accessKeySecret)
                    .build()
            )

            client = AsyncClient.builder()
                .region(smsRequest.region)
                .credentialsProvider(provider)
                .build()

            val sendSmsRequest = SendSmsRequest.builder()
                .phoneNumbers(smsRequest.phoneNumbers)
                .signName(smsRequest.signName)
                .templateCode(smsRequest.templateCode)
                .templateParam(smsRequest.templateParam)
                .build()

            LOG.info("[aliyun]开始发送短信...")
            val response = client.sendSms(sendSmsRequest)
            smsResponse = response.get()!!.body
            LOG.info("[aliyun]发送短信成功, 结果:{0}", JsonKit.toJson(smsResponse))
        } catch (e: Exception) {
            LOG.error(e, "[aliyun]发送短信失败")
        } finally {
            client?.close()
            //执行回调
            callback.invoke(smsResponse)
        }
    }

    private val LOG = LogFactory.getLog(this)

}
