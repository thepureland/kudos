package io.kudos.ms.user.common.vo.contact

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 用户联系方式查询记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserContactWayRecord (

    //region your codes 1

    /** 用户ID */
    var userId: String? = null,

    /** 联系方式字典码 */
    var contactWayDictCode: String? = null,

    /** 联系方式值 */
    var contactWayValue: String? = null,

    /** 联系方式状态字典码 */
    var contactWayStatusDictCode: String? = null,

    /** 优先级 */
    var priority: Short? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建者ID */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者ID */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}
