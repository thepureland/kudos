package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Update request VO for the message receive form.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveFormUpdate (

    /** Primary key */
    override val id: String,

    override val receiverId: String?,

    override val sendId: String?,

    override val receiveStatusDictCode: String?,

    override val createTime: LocalDateTime?,

    override val updateTime: LocalDateTime?,

    override val tenantId: String?,

) : IIdEntity<String>, IMsgReceiveFormBase
