package io.kudos.ams.sys.common.vo.dictitem

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 字典项查询记录
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysDictItemDetail : IdJsonResult<String>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2


    /** 字典项代码 */
    var itemCode: String? = null

    /** 字典项名称 */
    var itemName: String? = null

    /** 字典id */
    var dictId: String? = null

    /** 字典项排序 */
    var orderNum: Int? = null

    /** 父id */
    var parentId: String? = null

    /** 备注 */
    var remark: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    /** 是否内置 */
    var builtIn: Boolean? = null

    /** 创建用户 */
    var createUser: String? = null

    /** 创建时间 */
    var createTime: LocalDateTime? = null

    /** 更新用户 */
    var updateUser: String? = null

    /** 更新时间 */
    var updateTime: LocalDateTime? = null

}