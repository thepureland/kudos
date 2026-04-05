package io.kudos.ms.msg.common.template.vo
import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 消息模板缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class MsgTemplateCacheEntry (

    /** 主键 */
    override val id: String,

    /** 发送类型字典码 */
    val sendTypeDictCode: String?,

    /** 事件类型字典码 */
    val eventTypeDictCode: String?,

    /** 消息类型字典码 */
    val msgTypeDictCode: String?,

    /** 模板分组编码 */
    val receiverGroupCode: String?,

    /** 国家-语言字典码 */
    val localeDictCode: String?,

    /** 模板标题 */
    val title: String?,

    /** 模板内容 */
    val content: String?,

    /** 是否启用默认值 */
    val defaultActive: Boolean?,

    /** 模板标题默认值 */
    val defaultTitle: String?,

    /** 模板内容默认值 */
    val defaultContent: String?,

    /** 租户ID */
    val tenantId: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 5801009370756956314L
    }

}
