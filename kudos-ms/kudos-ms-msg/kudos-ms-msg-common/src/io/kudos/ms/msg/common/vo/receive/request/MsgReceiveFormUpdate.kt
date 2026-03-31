package io.kudos.ms.msg.common.vo.receive.request

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
    override val id: String? = null,

    override val receiverId: String? = null,

    override val sendId: String? = null,

    override val receiveStatusDictCode: String? = null,

    override val createTime: LocalDateTime? = null,

    override val updateTime: LocalDateTime? = null,

    override val tenantId: String? = null,

) : IIdEntity<String?>, IMsgReceiveFormBase
