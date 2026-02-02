package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 域名查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysDomainRecord::class,

    /** 域名 */
    var domain: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysDomainRecord::class)

    //endregion your codes 3

}