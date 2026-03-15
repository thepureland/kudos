package io.kudos.ms.msg.common.vo.receivergroup

import io.kudos.base.model.payload.FormPayload


/**
 * 消息接收者群组表单载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgReceiverGroupForm (

    /** 主键 */
    override val id: String = "",


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

) : FormPayload<String>() {


    constructor() : this("")


}
