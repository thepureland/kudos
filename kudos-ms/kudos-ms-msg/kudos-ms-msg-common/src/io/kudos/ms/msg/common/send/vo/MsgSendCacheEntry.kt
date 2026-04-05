package io.kudos.ms.msg.common.send.vo
import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 消息发送缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class MsgSendCacheEntry (

    /** 主键 */
    override val id: String,

    /** 接收者群组类型字典码 */
    val receiverGroupTypeDictCode: String?,

    /** 接收者群组ID */
    val receiverGroupId: String?,

    /** 消息实例ID */
    val instanceId: String?,

    /** 消息类型字典码 */
    val msgTypeDictCode: String?,

    /** 国家-语言字典码 */
    val localeDictCode: String?,

    /** 发送状态字典码 */
    val sendStatusDictCode: String?,

    /** 创建时间 */
    val createTime: LocalDateTime?,

    /** 更新时间 */
    val updateTime: LocalDateTime?,

    /** 发送成功数量 */
    val successCount: Int?,

    /** 发送失败数量 */
    val failCount: Int?,

    /** 定时任务ID */
    val jobId: String?,

    /** 租户ID */
    val tenantId: String?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 6949395793750221523L
    }

}
