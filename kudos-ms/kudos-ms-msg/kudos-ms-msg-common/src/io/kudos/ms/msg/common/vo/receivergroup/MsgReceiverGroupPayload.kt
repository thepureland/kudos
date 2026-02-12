package io.kudos.ms.msg.common.vo.receivergroup

import io.kudos.base.support.payload.FormPayload


/**
 * 消息接收者群组表单载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgReceiverGroupPayload (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

    /** 接收者群组类型字典码 */
    var receiverGroupTypeDictCode: String? = null,

    /** 群组定义的表 */
    var defineTable: String? = null,

    /** 群组名称在具体群组表中的字段名 */
    var nameColumn: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}
