package io.kudos.ms.msg.common.vo.receivergroup

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 消息接收者群组查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgReceiverGroupSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = MsgReceiverGroupRecord::class,

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

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(MsgReceiverGroupRecord::class)

    //endregion your codes 3

}
