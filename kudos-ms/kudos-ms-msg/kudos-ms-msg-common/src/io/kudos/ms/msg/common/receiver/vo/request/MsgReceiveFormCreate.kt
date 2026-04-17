package io.kudos.ms.msg.common.receiver.vo.request

import java.time.LocalDateTime


/**
 * 消息接收表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveFormCreate (

    override val receiverId: String? ,

    override val sendId: String? ,

    override val receiveStatusDictCode: String? ,

    override val createTime: LocalDateTime? ,

    override val updateTime: LocalDateTime? ,

    override val tenantId: String? ,

) : IMsgReceiveFormBase
