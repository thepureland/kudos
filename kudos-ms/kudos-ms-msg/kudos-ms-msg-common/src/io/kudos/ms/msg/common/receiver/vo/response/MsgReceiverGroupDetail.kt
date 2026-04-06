package io.kudos.ms.msg.common.receiver.vo.response
import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 消息接收者群组详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupDetail (

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

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>