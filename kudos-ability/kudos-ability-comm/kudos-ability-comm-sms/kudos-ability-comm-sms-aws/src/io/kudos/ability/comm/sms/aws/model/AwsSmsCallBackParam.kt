package io.kudos.ability.comm.sms.aws.model

import java.io.Serial
import java.io.Serializable

/**
 * AWS SNS SMS send callback payload.
 *
 * - Normal 2xx: [statusCode] = 200, [messageId] / [sequenceNumber] returned by AWS
 * - AwsServiceException / SdkServiceException: [statusCode] comes from the SDK; [statusText] is
 *   the error code or description
 * - Local exception (connection failure, serialization errors, etc.): [statusCode] = 599 +
 *   "client error" (handled as a fallback by the handler)
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
