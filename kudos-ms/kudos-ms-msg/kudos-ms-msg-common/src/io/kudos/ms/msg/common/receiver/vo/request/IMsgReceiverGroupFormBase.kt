package io.kudos.ms.msg.common.receiver.vo.request
import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * 消息接收者群组表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgReceiverGroupFormBase {

    /** 接收者群组类型字典码 */
    val receiverGroupTypeDictCode: String?

    /** 群组定义的表 */
    val defineTable: String?

    /** 群组名称在具体群组表中的字段名 */
    val nameColumn: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?

    /** 是否启用 */
    val active: Boolean?
}
