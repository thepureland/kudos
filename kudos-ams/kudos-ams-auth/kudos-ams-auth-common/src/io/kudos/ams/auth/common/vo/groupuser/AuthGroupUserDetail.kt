package io.kudos.ams.auth.common.vo.groupuser

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 组-用户关系查询记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class AuthGroupUserDetail (

    //region your codes 1

    /** 组id */
    var groupId: String? = null,

    /** 用户id */
    var userId: String? = null,

    /** 创建者id */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者id */
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

    constructor() : this(null)

    //endregion your codes 3

}
