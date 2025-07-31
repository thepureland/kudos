package io.kudos.ams.sys.common.vo.tenant

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 租户查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysTenantRecord::class,

    /** 名称 */
    var name: String? = null,

    var subSystemCode: String? = null,

    /** 时区 */
    var timezone: String? = null,

    /** 默认语言编码 */
    var defaultLanguageCode: String? = null,

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

    constructor() : this(SysTenantRecord::class)

    //endregion your codes 3

}