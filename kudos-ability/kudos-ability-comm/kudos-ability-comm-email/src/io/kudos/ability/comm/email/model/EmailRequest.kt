package io.kudos.ability.comm.email.model

import java.io.Serial
import java.io.Serializable

/**
 * Email send request body.
 *
 * For field semantics see the inline kdoc on each var; the typical usage is for the business layer
 * to populate it and pass it to [io.kudos.ability.comm.email.handler.EmailHandler.send].
 *
 * **Do not log instances of this class** - `senderPassword` is plaintext and will be exposed once
 * serialized.
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
class EmailRequest : Serializable {
    /**
     * Protocol used to send the email.
     */
    var protocol: String = "smtp"

    /**
     * Email subject.
     */
    var subject: String? = null

    /**
     * Email body.
     */
    var body: String? = null

    /**
     * Email recipients.
     */
    var receivers = mutableSetOf<String>()

    /**
     * Sender email account.
     */
    var senderAccount: String? = null

    /**
     * Sender email password.
     */
    var senderPassword: String? = null

    /**
     * Mail server address of the sender's mailbox.
     */
    var serverHost: String? = null

    /**
     * Mail server port of the sender's mailbox.
     */
    var serverPort: Int? = null

    /**
     * Whether SMTP server authentication is required.
     */
    var smtpAuth: Boolean = true

    /**
     * Configure partial sending (when some recipient addresses in the list are invalid, ignore those
     * invalid addresses). Default true.
     */
    var sendpartial: Boolean = true

    /**
     * Whether to send in HTML format.
     */
    var htmlFormat: Boolean = true

    /**
     * Email body encoding.
     */
    var encoding: String? = "UTF-8"

    /**
     * SSL encryption.
     */
    var ssl: Boolean = true

    /**
     * Extra information.
     */
    var extra: MutableMap<String, String>? = null

    /**
     * Sender email address shown in the email header `From`.
     * Distinct from [senderAccount]: [senderAccount] is the account used to authenticate against
     * SMTP, while [fromMailAddress] is the source address the recipient actually sees.
     * If left empty, most SMTP servers fall back to [senderAccount].
     */
    var fromMailAddress: String? = null

    companion object {
        /** Serializable version UID. */
        @Serial
        private val serialVersionUID = -6829180589038163995L
    }
}
