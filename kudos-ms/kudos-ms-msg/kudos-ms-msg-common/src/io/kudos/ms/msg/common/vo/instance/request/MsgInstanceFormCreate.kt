package io.kudos.ms.msg.common.vo.instance.request

import java.time.LocalDateTime


/**
 * 消息实例表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgInstanceFormCreate (

    override val localeDictCode: String? = null,

    override val title: String? = null,

    override val content: String? = null,

    override val templateId: String? = null,

    override val sendTypeDictCode: String? = null,

    override val eventTypeDictCode: String? = null,

    override val msgTypeDictCode: String? = null,

    override val validTimeStart: LocalDateTime? = null,

    override val validTimeEnd: LocalDateTime? = null,

    override val tenantId: String? = null,

) : IMsgInstanceFormBase
