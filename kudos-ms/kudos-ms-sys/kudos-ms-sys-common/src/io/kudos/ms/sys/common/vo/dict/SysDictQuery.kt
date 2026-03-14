package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0


/**
 * 字典查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictQuery (


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

) : ListSearchPayload() {


    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysDictRow::class

    override val operators: Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::dictType to OperatorEnum.ILIKE,
        ::dictName to OperatorEnum.ILIKE,
    )


}