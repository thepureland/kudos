package io.kudos.ms.user.common.vo.contact

import io.kudos.base.model.contract.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 用户联系方式查询记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserContactWayRow (


    /** 用户ID */
    val userId: String? = null,

    /** 联系方式字典码 */
    val contactWayDictCode: String? = null,

    /** 联系方式值 */
    val contactWayValue: String? = null,

    /** 联系方式状态字典码 */
    val contactWayStatusDictCode: String? = null,

    /** 优先级 */
    val priority: Short? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 创建者ID */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者ID */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IdJsonResult<String>() {


    constructor() : this("")


}
