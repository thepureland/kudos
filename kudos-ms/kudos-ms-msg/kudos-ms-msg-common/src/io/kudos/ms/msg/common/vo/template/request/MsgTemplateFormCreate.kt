package io.kudos.ms.msg.common.vo.template.request

/**
 * 消息模板表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateFormCreate (

    /** 发送类型字典码 */
    val sendTypeDictCode: String? = null,

    /** 事件类型字典码 */
    val eventTypeDictCode: String? = null,

    /** 消息类型字典码 */
    val msgTypeDictCode: String? = null,

    /** 模板分组编码 */
    val receiverGroupCode: String? = null,

    /** 国家-语言字典码 */
    val localeDictCode: String? = null,

    /** 模板标题 */
    val title: String? = null,

    /** 模板内容 */
    val content: String? = null,

    /** 是否启用默认值 */
    val defaultActive: Boolean? = null,

    /** 模板标题默认值 */
    val defaultTitle: String? = null,

    /** 模板内容默认值 */
    val defaultContent: String? = null,

    /** 租户ID */
    val tenantId: String? = null,

)
