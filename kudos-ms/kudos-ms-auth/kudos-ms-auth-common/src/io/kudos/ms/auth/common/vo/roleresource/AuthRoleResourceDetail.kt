package io.kudos.ms.auth.common.vo.roleresource

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 角色-资源关系查询记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class AuthRoleResourceDetail (

    //region your codes 1

    /** 角色id */
    var roleId: String? = null,

    /** 资源id */
    var resourceId: String? = null,

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

    constructor() : this("")

    //endregion your codes 3

}
