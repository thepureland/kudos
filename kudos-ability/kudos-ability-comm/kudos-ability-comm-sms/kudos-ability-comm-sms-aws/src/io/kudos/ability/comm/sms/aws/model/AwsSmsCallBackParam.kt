package io.kudos.ability.comm.sms.aws.model

import java.io.Serial
import java.io.Serializable

/**
 * AWS SNS 短信发送回调载体。
 *
 * - 正常 2xx：[statusCode] = 200, [messageId] / [sequenceNumber] 由 AWS 返回
 * - AwsServiceException / SdkServiceException：[statusCode] 来自 SDK，[statusText] 是错误码或描述
 * - 本地异常（连接失败、序列化错误等）：[statusCode] = 599 + "client error"（由 handler 兜底）
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class AwsSmsCallBackParam : Serializable {
    var statusCode: Int = 0
    var statusText: String? = null
    var messageId: String? = null
    var sequenceNumber: String? = null

    override fun toString(): String =
        "AwsSmsResponse(statusCode=$statusCode, statusText='$statusText', messageId='$messageId', sequenceNumber='$sequenceNumber')"

    companion object {
        @Serial
        private const val serialVersionUID = 768228150028246922L
    }
}
