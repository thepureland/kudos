package io.kudos.ms.msg.common.instance.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import java.time.LocalDateTime
import io.kudos.ms.msg.common.instance.vo.response.MsgInstanceRow


/**
 * Message instance list query request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgInstanceQuery (

    /** Country-language dict code. */
    val localeDictCode: String? = null,

    /** Title. */
    val title: String? = null,

    /** Notification content. */
    val content: String? = null,

    /** Message template id. */
    val templateId: String? = null,

    /** Send type dict code. */
    val sendTypeDictCode: String? = null,

    /** Event type dict code. */
    val eventTypeDictCode: String? = null,

    /** Message type dict code. */
    val msgTypeDictCode: String? = null,

    /** Valid time start. */
    val validTimeStart: LocalDateTime? = null,

    /** Valid time end. */
    val validTimeEnd: LocalDateTime? = null,

    /** Tenant id. */
    val tenantId: String? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = MsgInstanceRow::class

}