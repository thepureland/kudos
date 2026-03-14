package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 字典查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictQuery (

    //region your codes 1

    val id: String? = null,

    /** 字典类型 */
    val dictType: String? = null,

    /** 字典名称 */
    val dictName: String? = null,

    /** 原子服务编码 */
    val atomicServiceCode: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysDictRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(
        ::dictType.name to OperatorEnum.ILIKE,
        ::dictName.name to OperatorEnum.ILIKE,
    )

    //endregion your codes 3

}