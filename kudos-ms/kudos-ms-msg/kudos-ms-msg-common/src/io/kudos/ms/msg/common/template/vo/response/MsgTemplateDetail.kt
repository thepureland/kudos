package io.kudos.ms.msg.common.template.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Message template detail response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateDetail (

    /** Primary key */
    override val id: String = "",

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

) : IIdEntity<String>