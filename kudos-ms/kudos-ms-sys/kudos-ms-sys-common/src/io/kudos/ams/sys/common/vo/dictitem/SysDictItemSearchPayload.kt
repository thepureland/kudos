package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 字典项查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysDictItemRecord::class,

    /** 主键 */
    var id: String? = null,

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

    /** 是否为第一层树节点 */
    var firstLevel: Boolean? = null,


    /** 字典类型 */
    var dictType: String? = null,

    /** 字典名称 */
    var dictName: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 字典是否启用 */
    var dictActive: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysDictItemRecord::class)

    //endregion your codes 3

}