package io.kudos.ms.msg.common.vo.send.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 消息发送表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgSendFormUpdate (

    /** 主键 */
    override val id: String? = null,

    override val receiverGroupTypeDictCode: String? = null,

    override val receiverGroupId: String? = null,

    override val instanceId: String? = null,

    override val msgTypeDictCode: String? = null,

    override val localeDictCode: String? = null,

    override val sendStatusDictCode: String? = null,

    override val createTime: LocalDateTime? = null,

    override val updateTime: LocalDateTime? = null,

    override val successCount: Int? = null,

    override val failCount: Int? = null,

    override val jobId: String? = null,

    override val tenantId: String? = null,

) : IIdEntity<String?>, IMsgSendFormBase
