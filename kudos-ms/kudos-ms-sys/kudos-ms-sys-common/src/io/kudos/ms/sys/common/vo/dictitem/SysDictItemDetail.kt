package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 字典项查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemDetail (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

    /** 字典项代码 */
    var itemCode: String? = null,

    /** 字典项名称 */
    var itemName: String? = null,

    /** 字典id */
    var dictId: String? = null,

    /** 字典项排序 */
    var orderNum: Int? = null,

    /** 父id */
    var parentId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
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