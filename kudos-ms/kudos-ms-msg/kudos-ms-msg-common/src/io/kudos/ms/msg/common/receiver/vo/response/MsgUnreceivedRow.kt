package io.kudos.ms.msg.common.receiver.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 未送达消息列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgUnreceivedRow(

    override val id: String = "",

    val receiverId: String? = null,
    val sendId: String? = null,
    val publishMethodDictCode: String? = null,
    val failReason: String? = null,
    val retryCount: Int? = null,
    val lastRetryTime: LocalDateTime? = null,
    val resolved: Boolean? = null,
    val createTime: LocalDateTime? = null,
    val updateTime: LocalDateTime? = null,
    val tenantId: String? = null,

) : IIdEntity<String>
