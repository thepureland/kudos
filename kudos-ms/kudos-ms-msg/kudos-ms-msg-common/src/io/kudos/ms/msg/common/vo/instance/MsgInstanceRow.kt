package io.kudos.ms.msg.common.vo.instance

import io.kudos.base.model.contract.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 消息实例查询记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgInstanceRow (

    /** 主键 */
    override val id: String = "",


    /** 国家-语言字典码 */
    val localeDictCode: String? = null,

    /** 标题 */
    val title: String? = null,

    /** 通知内容 */
    val content: String? = null,

    /** 消息模板id */
    val templateId: String? = null,

    /** 发送类型字典码 */
    val sendTypeDictCode: String? = null,

    /** 事件类型字典码 */
    val eventTypeDictCode: String? = null,

    /** 消息类型字典码 */
    val msgTypeDictCode: String? = null,

    /** 有效期起 */
    val validTimeStart: LocalDateTime? = null,

    /** 有效期止 */
    val validTimeEnd: LocalDateTime? = null,

    /** 租户ID */
    val tenantId: String? = null,

) : IdJsonResult<String>() {


    constructor() : this("")


}
