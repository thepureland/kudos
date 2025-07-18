package io.kudos.ams.sys.common.vo.cache

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 缓存查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysCacheSearchPayload : ListSearchPayload() {
//endregion your codes 1

    //region your codes 2

    /** 名称 */
    var name: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    //endregion your codes 2

    override var returnEntityClass: KClass<*>? = SysCacheRecord::class

}