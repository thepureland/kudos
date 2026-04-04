package io.kudos.ms.msg.common.vo.receive

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 消息接收缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveCacheEntry (

    /** 主键 */
    override val id: String,

    /** 接收者ID */
    val receiverId: String?,

    /** 发送ID */
    val sendId: String?,

    /** 接收状态字典码 */
    val receiveStatusDictCode: String?,

    /** 创建时间 */
    val createTime: LocalDateTime?,

    /** 更新时间 */
    val updateTime: LocalDateTime?,

    /** 租户ID */
    val tenantId: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8705640601695840987L
    }

}
