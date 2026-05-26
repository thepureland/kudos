package io.kudos.ms.sys.common.cache.vo.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import io.kudos.ms.sys.common.cache.vo.response.SysCacheRow
import kotlin.reflect.KProperty0


/**
 * Cache list query request VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheQuery (

    /** Name */
    val name: String? = null,

    /** Atomic service code */
    val atomicServiceCode: String? = null,

    /** Cache strategy code */
    @get:DictItemCode(dictType = SysDictTypes.CACHE_STRATEGY, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val strategyDictCode: String? = null,

    /** Whether it is a Hash cache */
    val hash: Boolean? = null,

    /** Only enabled */
    val active: Boolean? = true,

) : ListSearchPayload() {

//    constructor() : this(null)

    override fun getReturnEntityClass() = SysCacheRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::name to OperatorEnum.ILIKE)

    override fun isUnpagedSearchAllowed(): Boolean = true

}