package io.kudos.ms.msg.common.vo.send

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 消息发送缓存项
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgSendCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 接收者群组类型字典码 */
    val receiverGroupTypeDictCode: String? = null,

    /** 接收者群组ID */
    val receiverGroupId: String? = null,

    /** 消息实例ID */
    val instanceId: String? = null,

    /** 消息类型字典码 */
    val msgTypeDictCode: String? = null,

    /** 国家-语言字典码 */
    val localeDictCode: String? = null,

    /** 发送状态字典码 */
    val sendStatusDictCode: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

    /** 发送成功数量 */
    val successCount: Int? = null,

    /** 发送失败数量 */
    val failCount: Int? = null,

    /** 定时任务ID */
    val jobId: String? = null,

    /** 租户ID */
    val tenantId: String? = null,

) : IIdEntity<String>, Serializable {




    companion object {
        private const val serialVersionUID = 6949395793750221523L
    }

}