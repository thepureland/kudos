package io.kudos.ability.comm.sms.aws.model

import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import java.io.Serial
import java.io.Serializable

/**
 * AWS SNS 短信发送请求体。字段对应 AWS SDK `PublishRequest`；同时携带本次调用使用的 IAM 凭证
 * ([accessKeyId] / [accessKeySecret]) —— 支持"多租户各自一套 AWS IAM"的场景。
 *
 * **不要把本类实例输出到日志** —— [accessKeySecret] 是明文 + [Serializable]，可能被日志 / 缓存序列化时外泄。
 *
 * @author paul
 * @author K
 * @since 1.0.0
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
