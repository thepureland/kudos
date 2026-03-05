package io.kudos.ms.sys.common.vo.cache

import io.kudos.base.bean.validation.constraint.annotations.DictCode
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
data class SysCacheSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysCacheRecord::class,

    /** 名称 */
    var name: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 缓存策略代码 */
    @get:DictCode(dictType = SysDictTypes.CACHE_STRATEGY, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    var strategyDictCode: String? = null,

    /** 是否为Hash缓存 */
    var hash: Boolean? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysCacheRecord::class)

    //endregion your codes 3

}