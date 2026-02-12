package io.kudos.ms.auth.common.vo.role

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 角色查询记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class AuthRoleDetail (

    //region your codes 1

    /** 角色编码 */
    var code: String? = null,

    /** 角色名称 */
    var name: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 子系统编码 */
    var subsysCode: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否激活 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

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
