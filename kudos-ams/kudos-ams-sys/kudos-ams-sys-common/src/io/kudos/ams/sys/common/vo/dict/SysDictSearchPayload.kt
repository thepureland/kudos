package io.kudos.ams.sys.common.vo.dict

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 字典查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysDictSearchPayload : ListSearchPayload() {
//endregion your codes 1

    //region your codes 2

    /** 主键 */
    var id: String? = null

    /** 模块 */
    var module: String? = null

    /** 字典类型 */
    var dictType: String? = null

    /** 字典名称，或其国际化key */
    var dictName: String? = null

    /** 字典项编号 */
    var itemCode: String? = null

    /** 父项编号 */
    var parentCode: String? = null

    /** 父项主键 */
    var parentId: String? = null

    /** 字典项名称，或其国际化key */
    var itemName: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    /** 是否为第一层树节点 */
    var firstLevel: Boolean? = null

    /** 是否只查询Dict */
    var isDict: Boolean = false

    /** 字典id */
    var dictId: String? = null

    //endregion your codes 2

    override var returnEntityClass: KClass<*>? = SysDictRecord::class

}