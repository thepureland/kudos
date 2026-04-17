package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 消息接收者群组编辑响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupEdit (

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

) : IIdEntity<String>
