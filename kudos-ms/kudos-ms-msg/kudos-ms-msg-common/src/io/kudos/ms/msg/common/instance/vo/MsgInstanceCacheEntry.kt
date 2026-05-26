package io.kudos.ms.msg.common.instance.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Message instance cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgInstanceCacheEntry (

    /** Primary key */
    override val id: String,

    /** Country-language dictionary code */
    val localeDictCode: String?,

    /** Title */
    val title: String?,

    /** Notification content */
    val content: String?,

    /** Message template id */
    val templateId: String?,

    /** Send type dictionary code */
    val sendTypeDictCode: String?,

    /** Event type dictionary code */
    val eventTypeDictCode: String?,

    /** Message type dictionary code */
    val msgTypeDictCode: String?,

    /** Validity start time */
    val validTimeStart: LocalDateTime?,

    /** Validity end time */
    val validTimeEnd: LocalDateTime?,

    /** Tenant ID */
    val tenantId: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 5744943131449847637L
    }

}
