package io.kudos.ms.msg.common.vo.receivergroup

import io.kudos.base.support.result.IdJsonResult


/**
 * 消息接收者群组查询记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgReceiverGroupRow (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 接收者群组类型字典码 */
    val receiverGroupTypeDictCode: String? = null,

    /** 群组定义的表 */
    val defineTable: String? = null,

    /** 群组名称在具体群组表中的字段名 */
    val nameColumn: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}
