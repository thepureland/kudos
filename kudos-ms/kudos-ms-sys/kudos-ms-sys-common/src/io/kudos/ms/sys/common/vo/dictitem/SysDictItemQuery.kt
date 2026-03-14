package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0


/**
 * 字典项查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemQuery (


    /** 主键 */
    val id: String? = null,

    /** 字典项代码 */
    val itemCode: String? = null,

    /** 字典项名称 */
    val itemName: String? = null,

    /** 字典id */
    val dictId: String? = null,

    /** 字典项排序 */
    val orderNum: Int? = null,

    /** 父id */
    var parentId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 字典类型 */
    val dictType: String? = null,

    /** 字典名称 */
    val dictName: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 字典是否启用 */
    val dictActive: Boolean? = null,

) : ListSearchPayload() {


    constructor() : this("")

    override fun getReturnEntityClass() = SysDictItemRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::dictType to OperatorEnum.ILIKE,
        ::dictName to OperatorEnum.ILIKE,
        ::itemCode to OperatorEnum.ILIKE,
        ::itemName to OperatorEnum.ILIKE,
    )


}