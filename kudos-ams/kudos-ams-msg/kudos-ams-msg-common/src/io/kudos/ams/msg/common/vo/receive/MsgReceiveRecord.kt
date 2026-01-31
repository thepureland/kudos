package io.kudos.ams.msg.common.vo.receive

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 消息接收查询记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgReceiveRecord (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 接收者ID */
    var receiverId: String? = null,

    /** 发送ID */
    var sendId: String? = null,

    /** 接收状态字典码 */
    var receiveStatusDictCode: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    /** 租户ID */
    var tenantId: String? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}
