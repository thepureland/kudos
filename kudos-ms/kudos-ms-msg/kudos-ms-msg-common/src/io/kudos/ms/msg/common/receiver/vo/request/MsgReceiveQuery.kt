package io.kudos.ms.msg.common.receiver.vo.request
import io.kudos.base.model.payload.ListSearchPayload
import java.time.LocalDateTime
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveRow


/**
 * 消息接收列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class MsgReceiveQuery (

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

) : ListSearchPayload() {

    override fun getReturnEntityClass() = MsgReceiveRow::class

}