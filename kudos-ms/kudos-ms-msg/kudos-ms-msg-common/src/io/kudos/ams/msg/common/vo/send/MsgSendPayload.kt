package io.kudos.ms.msg.common.vo.send

import io.kudos.base.support.payload.FormPayload
import java.time.LocalDateTime


/**
 * 消息发送表单载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgSendPayload (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 接收者群组类型字典码 */
    var receiverGroupTypeDictCode: String? = null,

    /** 接收者群组ID */
    var receiverGroupId: String? = null,

    /** 消息实例ID */
    var instanceId: String? = null,

    /** 消息类型字典码 */
    var msgTypeDictCode: String? = null,

    /** 国家-语言字典码 */
    var localeDictCode: String? = null,

    /** 发送状态字典码 */
    var sendStatusDictCode: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    /** 发送成功数量 */
    var successCount: Int? = null,

    /** 发送失败数量 */
    var failCount: Int? = null,

    /** 定时任务ID */
    var jobId: String? = null,

    /** 租户ID */
    var tenantId: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}
