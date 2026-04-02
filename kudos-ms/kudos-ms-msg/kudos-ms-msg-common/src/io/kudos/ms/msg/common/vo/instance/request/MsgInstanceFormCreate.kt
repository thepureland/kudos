package io.kudos.ms.msg.common.vo.instance.request

import java.time.LocalDateTime


/**
 * 消息实例表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgInstanceFormCreate (

    override val localeDictCode: String? ,

    override val title: String? ,

    override val content: String? ,

    override val templateId: String? ,

    override val sendTypeDictCode: String? ,

    override val eventTypeDictCode: String? ,

    override val msgTypeDictCode: String? ,

    override val validTimeStart: LocalDateTime? ,

    override val validTimeEnd: LocalDateTime? ,

    override val tenantId: String? ,

) : IMsgInstanceFormBase
