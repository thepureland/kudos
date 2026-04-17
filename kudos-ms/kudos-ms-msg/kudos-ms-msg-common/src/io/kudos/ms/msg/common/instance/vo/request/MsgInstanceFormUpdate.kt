package io.kudos.ms.msg.common.instance.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 消息实例表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgInstanceFormUpdate (

    /** 主键 */
    override val id: String,

    override val localeDictCode: String?,

    override val title: String?,

    override val content: String?,

    override val templateId: String?,

    override val sendTypeDictCode: String?,

    override val eventTypeDictCode: String?,

    override val msgTypeDictCode: String?,

    override val validTimeStart: LocalDateTime?,

    override val validTimeEnd: LocalDateTime?,

    override val tenantId: String?,

) : IIdEntity<String>, IMsgInstanceFormBase
