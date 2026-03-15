package io.kudos.ms.auth.common.vo.roleuser

import io.kudos.base.model.contract.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 角色-用户关系查询记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class AuthRoleUserDetail (


    /** 角色id */
    val roleId: String? = null,

    /** 用户id */
    val userId: String? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IdJsonResult<String>() {


    constructor() : this("")


}
