package io.kudos.ams.sys.common.vo.tenant

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 租户查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysTenantSearchPayload : ListSearchPayload() {
//endregion your codes 1

    //region your codes 2

    /** 子系统代码 */
    var subSystemCode: String? = null

    /** 名称 */
    var name: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    //endregion your codes 2

    override var returnEntityClass: KClass<*>? = SysTenantRecord::class

}