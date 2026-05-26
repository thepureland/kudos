package io.kudos.ms.msg.common.instance.vo.request

import java.time.LocalDateTime

/**
 * Base fields of the message instance form (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgInstanceFormBase {

    /** Country-language dictionary code */
    val localeDictCode: String?

    /** Title */
    val title: String?

    /** Notification content */
    val content: String?

    /** Message template id */
    val templateId: String?

    /** Send type dictionary code */
    val sendTypeDictCode: String?

    /** Event type dictionary code */
    val eventTypeDictCode: String?

    /** Message type dictionary code */
    val msgTypeDictCode: String?

    /** Validity start time */
    val validTimeStart: LocalDateTime?

    /** Validity end time */
    val validTimeEnd: LocalDateTime?

    /** Tenant ID */
    val tenantId: String?
}
