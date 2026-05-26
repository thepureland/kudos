package io.kudos.ability.comm.email.model

import io.kudos.ability.comm.email.enums.EmailStatusEnum
import java.io.Serial
import java.io.Serializable

/**
 * Email send callback payload. The callback of `EmailHandler.send(...)` receives this object;
 * the business side uses it to write the "email send record" or trigger a resend.
 *
 * - [status] `SUCCESS` - all recipients were sent to
 * - [status] `SUCCESS_PART` - some recipients were sent to; [successEmails] / [failEmails] are both non-empty
 * - [status] `FAIL` - all failed ([failEmails] equals the original receivers)
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class EmailCallBackParam : Serializable {
    /**
     * Send status.
     */
    var status: EmailStatusEnum? = null

    /**
     * Email accounts that were sent successfully.
     */
    var successEmails: MutableSet<String>? = null

    /**
     * Email accounts that failed to send.
     */
    var failEmails: MutableSet<String>? = null

    override fun toString(): String {
        return "EmailCallBackParam{" +
                "status=" + status +
                ", successEmails=" + successEmails +
                ", failEmails=" + failEmails +
                '}'
    }

    companion object {
        @Serial
        private val serialVersionUID = -1651796105092981458L
    }
}
