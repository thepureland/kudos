package io.kudos.ms.msg.common.vo.send.request

import java.time.LocalDateTime


/**
 * 消息发送表单新建请求VO
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
