package io.kudos.ms.msg.common.receiver.enums


/**
 * Constants for undelivered failure reasons (written into `msg_unreceived.fail_reason`).
 *
 * Using an enum rather than free text avoids each failure row introducing a new spelling
 * and leaves room for later aggregation/statistics by reason. The admin UI can attach an
 * i18n key for translation when displaying.
 *
 * @author K
 * @since 1.0.0
 */
enum class MsgUnreceivedReasonEnum(val code: String) {

    /** User has no contact info configured for the target channel (e.g. sending email but the user has no email address) */
    NO_CONTACT("NO_CONTACT"),

    /** Channel server returned a failure (SMTP rejected, SMS API error, etc.) */
    CHANNEL_REJECT("CHANNEL_REJECT"),

    /** Channel call timed out */
    TIMEOUT("TIMEOUT"),

    /** Listener threw an exception during processing */
    LISTENER_ERROR("LISTENER_ERROR"),

    /** Receiver id set is empty */
    EMPTY_RECEIVERS("EMPTY_RECEIVERS"),

    /** Other / uncategorized */
    UNKNOWN("UNKNOWN");
}
