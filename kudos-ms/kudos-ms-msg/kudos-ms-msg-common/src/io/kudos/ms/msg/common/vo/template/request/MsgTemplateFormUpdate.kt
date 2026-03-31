package io.kudos.ms.msg.common.vo.template.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 消息模板表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val sendTypeDictCode: String? = null,

    override val eventTypeDictCode: String? = null,

    override val msgTypeDictCode: String? = null,

    override val receiverGroupCode: String? = null,

    override val localeDictCode: String? = null,

    override val title: String? = null,

    override val content: String? = null,

    override val defaultActive: Boolean? = null,

    override val defaultTitle: String? = null,

    override val defaultContent: String? = null,

    override val tenantId: String? = null,

) : IIdEntity<String?>, IMsgTemplateFormBase
