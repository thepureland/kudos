package io.kudos.ms.msg.common.send.vo.request

import java.time.LocalDateTime


/**
 * Message send create form request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class MsgSendFormCreate (

    override val receiverGroupTypeDictCode: String? ,

    override val receiverGroupId: String? ,

    override val instanceId: String? ,

    override val msgTypeDictCode: String? ,

    override val localeDictCode: String? ,

    override val sendStatusDictCode: String? ,

    override val createTime: LocalDateTime? ,

    override val updateTime: LocalDateTime? ,

    override val successCount: Int? ,

    override val failCount: Int? ,

    override val jobId: String? ,

    override val tenantId: String? ,

) : IMsgSendFormBase
