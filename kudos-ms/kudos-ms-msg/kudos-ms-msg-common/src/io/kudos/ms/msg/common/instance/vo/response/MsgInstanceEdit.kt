package io.kudos.ms.msg.common.instance.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Edit response VO for the message instance.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgInstanceEdit (

    /** Primary key */
    override val id: String = "",

    /** Country-language dictionary code */
    val localeDictCode: String? = null,

    /** Title */
    val title: String? = null,

    /** Notification content */
    val content: String? = null,

    /** Message template id */
    val templateId: String? = null,

    /** Send type dictionary code */
    val sendTypeDictCode: String? = null,

    /** Event type dictionary code */
    val eventTypeDictCode: String? = null,

    /** Message type dictionary code */
    val msgTypeDictCode: String? = null,

    /** Validity start time */
    val validTimeStart: LocalDateTime? = null,

    /** Validity end time */
    val validTimeEnd: LocalDateTime? = null,

    /** Tenant ID */
    val tenantId: String? = null,

) : IIdEntity<String>
