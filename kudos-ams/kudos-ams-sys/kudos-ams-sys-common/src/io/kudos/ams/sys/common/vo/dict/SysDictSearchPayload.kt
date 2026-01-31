package io.kudos.ams.sys.common.vo.dict

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 字典查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysDictRecord::class,

    var id: String? = null,

    /** 字典类型 */
    var dictType: String? = null,

    /** 字典名称 */
    var dictName: String? = null,

    /** 模块编码 */
    var atomicServiceCode: String? = null,

    var parentId: String? = null,

    var firstLevel: Boolean? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    var isDict: Boolean = true,

    var itemCode: String? = null,

    var itemName: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysDictRecord::class)

    //endregion your codes 3

}