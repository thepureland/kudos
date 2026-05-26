package io.kudos.ability.comm.sms.aws.model

import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import java.io.Serial
import java.io.Serializable

/**
 * AWS SNS SMS send request body. Fields correspond to the AWS SDK `PublishRequest`; it also carries
 * the IAM credentials ([accessKeyId] / [accessKeySecret]) used for the call - supporting the
 * "multi-tenant, each with its own AWS IAM" scenario.
 *
 * **Do not log instances of this class** - [accessKeySecret] is plaintext and [Serializable]
 * allows it to leak when the object is serialized to logs / caches.
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class AwsSmsRequest : Serializable {
    /**
     * Region.
     */
    var region: String? = null

    /**
     * accessKeyId.
     */
    var accessKeyId: String? = null

    /**
     * accessKeySecret.
     */
    var accessKeySecret: String? = null

    /**
     * Phone number to receive the SMS, for example, +1XXX5550100.
     */
    var phoneNumber: String? = null

    /**
     * SMS content.
     */
    var message: String? = null

    /**
     * Extra information.
     */
    var messageAttributes: MutableMap<String?, MessageAttributeValue?>? = null

    companion object {
        @Serial
        private const val serialVersionUID = 3126960293572610404L
    }
}
