package io.kudos.ms.msg.common.vo.receive

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 消息接收缓存项
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgReceiveCacheItem (

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
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

    companion object {
        private const val serialVersionUID = 8705640601695840987L
    }

}
