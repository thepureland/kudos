package io.kudos.ms.msg.common.vo.receive.request

import java.time.LocalDateTime

/**
 * 消息接收表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgReceiveFormBase {

    /** 接收者ID */
    val receiverId: String?

    /** 发送ID */
    val sendId: String?

    /** 接收状态字典码 */
    val receiveStatusDictCode: String?

    /** 创建时间 */
    val createTime: LocalDateTime?

    /** 更新时间 */
    val updateTime: LocalDateTime?

    /** 租户ID */
    val tenantId: String?
}
