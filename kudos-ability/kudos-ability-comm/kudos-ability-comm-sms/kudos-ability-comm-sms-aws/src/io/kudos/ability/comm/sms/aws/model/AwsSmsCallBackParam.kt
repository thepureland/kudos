package io.kudos.ability.comm.sms.aws.model

import java.io.Serial
import java.io.Serializable

/**
 * @Description AWS发送短信的响应体
 * @Author paul
 * @Date 2023/2/10 18:23
 */
class AwsSmsCallBackParam : Serializable {
    var statusCode: Int = 0
    var statusText: String? = null
    var messageId: String? = null
    var sequenceNumber: String? = null

    override fun toString(): String {
        return "AwsSmsResponse{" +
                "statusCode=" + statusCode +
                ", statusText='" + statusText + '\'' +
                ", messageId='" + messageId + '\'' +
                ", sequenceNumber='" + sequenceNumber + '\'' +
                '}'
    }

    companion object {
        @Serial
        private const val serialVersionUID = 768228150028246922L
    }
}
