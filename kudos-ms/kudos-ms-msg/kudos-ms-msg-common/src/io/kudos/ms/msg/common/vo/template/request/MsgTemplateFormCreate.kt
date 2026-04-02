package io.kudos.ms.msg.common.vo.template.request


/**
 * 消息模板表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateFormCreate (

    override val sendTypeDictCode: String? ,

    override val eventTypeDictCode: String? ,

    override val msgTypeDictCode: String? ,

    override val receiverGroupCode: String? ,

    override val localeDictCode: String? ,

    override val title: String? ,

    override val content: String? ,

    override val defaultActive: Boolean? ,

    override val defaultTitle: String? ,

    override val defaultContent: String? ,

    override val tenantId: String? ,

) : IMsgTemplateFormBase
