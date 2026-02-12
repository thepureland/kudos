package io.kudos.ms.msg.common.vo.instance

import io.kudos.base.support.payload.FormPayload
import java.time.LocalDateTime


/**
 * 消息实例表单载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgInstancePayload (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

    /** 国家-语言字典码 */
    var localeDictCode: String? = null,

    /** 标题 */
    var title: String? = null,

    /** 通知内容 */
    var content: String? = null,

    /** 消息模板id */
    var templateId: String? = null,

    /** 发送类型字典码 */
    var sendTypeDictCode: String? = null,

    /** 事件类型字典码 */
    var eventTypeDictCode: String? = null,

    /** 消息类型字典码 */
    var msgTypeDictCode: String? = null,

    /** 有效期起 */
    var validTimeStart: LocalDateTime? = null,

    /** 有效期止 */
    var validTimeEnd: LocalDateTime? = null,

    /** 租户ID */
    var tenantId: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}
