package io.kudos.ms.msg.common.template.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateRow


/**
 * Message template list query criteria request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateQuery (

    /** Send type dictionary code */
    val sendTypeDictCode: String? = null,

    /** Event type dictionary code */
    val eventTypeDictCode: String? = null,

    /** Message type dictionary code */
    val msgTypeDictCode: String? = null,

    /** Template group code */
    val receiverGroupCode: String? = null,

    /** Country-language dictionary code */
    val localeDictCode: String? = null,

    /** Template title */
    val title: String? = null,

    /** Template content */
    val content: String? = null,

    /** Whether default values are enabled */
    val defaultActive: Boolean? = null,

    /** Default template title */
    val defaultTitle: String? = null,

    /** Default template content */
    val defaultContent: String? = null,

    /** Tenant ID */
    val tenantId: String? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = MsgTemplateRow::class

}