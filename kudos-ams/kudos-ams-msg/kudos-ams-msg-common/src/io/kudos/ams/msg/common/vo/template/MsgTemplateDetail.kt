package io.kudos.ams.msg.common.vo.template

import io.kudos.base.support.result.IdJsonResult


/**
 * 消息模板详情记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgTemplateDetail (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 发送类型字典码 */
    var sendTypeDictCode: String? = null,

    /** 事件类型字典码 */
    var eventTypeDictCode: String? = null,

    /** 消息类型字典码 */
    var msgTypeDictCode: String? = null,

    /** 模板分组编码 */
    var receiverGroupCode: String? = null,

    /** 国家-语言字典码 */
    var localeDictCode: String? = null,

    /** 模板标题 */
    var title: String? = null,

    /** 模板内容 */
    var content: String? = null,

    /** 是否启用默认值 */
    var defaultActive: Boolean? = null,

    /** 模板标题默认值 */
    var defaultTitle: String? = null,

    /** 模板内容默认值 */
    var defaultContent: String? = null,

    /** 租户ID */
    var tenantId: String? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}
