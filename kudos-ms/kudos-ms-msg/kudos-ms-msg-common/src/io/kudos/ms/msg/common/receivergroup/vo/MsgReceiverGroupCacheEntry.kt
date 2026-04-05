package io.kudos.ms.msg.common.receivergroup.vo
import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 消息接收者群组缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiverGroupCacheEntry (

    /** 主键 */
    override val id: String,

    /** 接收者群组类型字典码 */
    val receiverGroupTypeDictCode: String?,

    /** 群组定义的表 */
    val defineTable: String?,

    /** 群组名称在具体群组表中的字段名 */
    val nameColumn: String?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean?,

    /** 是否内置 */
    val builtIn: Boolean?,

    /** 创建者id */
    val createUserId: String?,

    /** 创建者名称 */
    val createUserName: String?,

    /** 创建时间 */
    val createTime: LocalDateTime?,

    /** 更新者id */
    val updateUserId: String?,

    /** 更新者名称 */
    val updateUserName: String?,

    /** 更新时间 */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 4762236923181019117L
    }

}
