package io.kudos.ms.msg.common.template.vo.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 消息模板表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateFormUpdate (

    /** 主键 */
    override val id: String,

    override val sendTypeDictCode: String?,

    override val eventTypeDictCode: String?,

    override val msgTypeDictCode: String?,

    override val receiverGroupCode: String?,

    override val localeDictCode: String?,

    override val title: String?,

    override val content: String?,

    override val defaultActive: Boolean?,

    override val defaultTitle: String?,

    override val defaultContent: String?,

    override val tenantId: String?,

) : IIdEntity<String>, IMsgTemplateFormBase
