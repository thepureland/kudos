package io.kudos.ms.msg.common.vo.receivergroup.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 消息接收者群组表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 接收者群组类型字典码 */
    val receiverGroupTypeDictCode: String? = null,

    /** 群组定义的表 */
    val defineTable: String? = null,

    /** 群组名称在具体群组表中的字段名 */
    val nameColumn: String? = null,

    /** 备注 */
    @get:MaxLength(128)
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

) : IIdEntity<String?>
