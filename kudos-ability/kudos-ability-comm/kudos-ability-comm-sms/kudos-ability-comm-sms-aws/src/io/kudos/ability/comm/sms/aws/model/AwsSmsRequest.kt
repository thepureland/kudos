package io.kudos.ability.comm.sms.aws.model

import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import java.io.Serial
import java.io.Serializable

/**
 * @Description AWS发送短信的请求体
 * @Author paul
 * @Date 2023/2/10 16:41
 */
class AwsSmsRequest : Serializable {
    /**
     * 区域
     */
    var region: String? = null

    /**
     * accessKeyId
     */
    var accessKeyId: String? = null

    /**
     * accessKeySecret
     */
    var accessKeySecret: String? = null

    /**
     * 接收短信的手机号码, for example, +1XXX5550100
     */
    var phoneNumber: String? = null

    /**
     * 短信内容
     */
    var message: String? = null

    /**
     * 扩展信息
     */
    var messageAttributes: MutableMap<String?, MessageAttributeValue?>? = null

    companion object {
        @Serial
        private const val serialVersionUID = 3126960293572610404L
    }
}
