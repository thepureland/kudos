package io.kudos.ms.msg.common.vo.template.request


/**
 * 消息模板表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateFormCreate (

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

) : IMsgTemplateFormBase
