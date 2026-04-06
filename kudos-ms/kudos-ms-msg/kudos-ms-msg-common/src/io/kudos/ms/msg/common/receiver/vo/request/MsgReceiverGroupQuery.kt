package io.kudos.ms.msg.common.receiver.vo.request
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupRow


/**
 * 消息接收者群组列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupQuery (

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

) : ListSearchPayload() {

    override fun getReturnEntityClass() = MsgReceiverGroupRow::class

}