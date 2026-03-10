package io.kudos.ms.sys.common.vo.cache

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.consts.SysDictTypes
import kotlin.reflect.KClass


/**
 * 缓存查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheQuery (

    //region your codes 1

    /** 名称 */
    val name: String? = null,

    /** 原子服务编码 */
    val atomicServiceCode: String? = null,

    /** 缓存策略代码 */
    @get:DictItemCode(dictType = SysDictTypes.CACHE_STRATEGY, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val strategyDictCode: String? = null,

    /** 是否为Hash缓存 */
    val hash: Boolean? = null,

    /** 仅启用 */
    val active: Boolean? = true,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    override var returnEntityClass: KClass<*>? = SysCacheRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(::name.name to OperatorEnum.ILIKE)

    //endregion your codes 3

}