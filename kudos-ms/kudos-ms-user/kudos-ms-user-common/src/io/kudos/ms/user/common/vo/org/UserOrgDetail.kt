package io.kudos.ms.user.common.vo.org

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 机构查询记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class UserOrgDetail (

    //region your codes 1

    /** 机构名称 */
    var name: String? = null,

    /** 机构简称 */
    var shortName: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 父机构id */
    var parentId: String? = null,

    /** 机构类型字典码 */
    var orgTypeDictCode: String? = null,

    /** 排序号 */
    var sortNum: Int? = null,

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

    constructor() : this(null)

    //endregion your codes 3

}
