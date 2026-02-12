package io.kudos.ms.user.common.vo.orguser

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 机构-用户关系查询记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class UserOrgUserDetail (

    //region your codes 1

    /** 机构id */
    var orgId: String? = null,

    /** 用户id */
    var userId: String? = null,

    /** 是否为机构管理员 */
    var orgAdmin: Boolean? = null,

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
