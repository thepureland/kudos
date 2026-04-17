package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 消息接收列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveRow (

    /** 主键 */
    override val id: String = "",

    /** 接收者ID */
    val receiverId: String? = null,

    /** 发送ID */
    val sendId: String? = null,

    /** 接收状态字典码 */
    val receiveStatusDictCode: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

    /** 租户ID */
    val tenantId: String? = null,

) : IIdEntity<String>