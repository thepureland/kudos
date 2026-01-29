package io.kudos.ams.user.common.vo.dept

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 部门树节点记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class AuthDeptTreeRecord (

    //region your codes 1

    /** 主键 */
    override var id: String? = null,

    /** 部门名称 */
    var name: String? = null,

    /** 部门简称 */
    var shortName: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 父部门id */
    var parentId: String? = null,

    /** 部门类型字典码 */
    var deptTypeDictCode: String? = null,

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

    /** 子部门列表 */
    var children: MutableList<AuthDeptTreeRecord>? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}
