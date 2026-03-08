package io.kudos.ms.msg.common.vo.receive

import io.kudos.base.support.payload.ListSearchPayload
import java.time.LocalDateTime
import kotlin.reflect.KClass


/**
 * 消息接收查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgReceiveSearchPayload (

    //region your codes 1

    /** 接收者ID */
    val receiverId: String? = null,

    /** 发送ID */
    val sendId: String? = null,

    /** 接收状态字典码 */
    val receiveStatusDictCode: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

    /** 租户ID */
    val tenantId: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = MsgReceiveRecord::class

    //endregion your codes 3

}
