package io.kudos.ms.msg.common.receive.vo.request
import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 消息接收表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveFormUpdate (

    /** 主键 */
    override val id: String,

    override val receiverId: String?,

    override val sendId: String?,

    override val receiveStatusDictCode: String?,

    override val createTime: LocalDateTime?,

    override val updateTime: LocalDateTime?,

    override val tenantId: String?,

) : IIdEntity<String>, IMsgReceiveFormBase
