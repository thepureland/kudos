package io.kudos.ms.msg.common.instance.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 消息实例缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class MsgInstanceCacheEntry (

    /** 主键 */
    override val id: String,

    /** 国家-语言字典码 */
    val localeDictCode: String?,

    /** 标题 */
    val title: String?,

    /** 通知内容 */
    val content: String?,

    /** 消息模板id */
    val templateId: String?,

    /** 发送类型字典码 */
    val sendTypeDictCode: String?,

    /** 事件类型字典码 */
    val eventTypeDictCode: String?,

    /** 消息类型字典码 */
    val msgTypeDictCode: String?,

    /** 有效期起 */
    val validTimeStart: LocalDateTime?,

    /** 有效期止 */
    val validTimeEnd: LocalDateTime?,

    /** 租户ID */
    val tenantId: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 5744943131449847637L
    }

}
