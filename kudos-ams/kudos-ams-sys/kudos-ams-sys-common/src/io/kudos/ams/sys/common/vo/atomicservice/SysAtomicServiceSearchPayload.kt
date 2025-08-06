package io.kudos.ams.sys.common.vo.atomicservice

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 原子服务查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysAtomicServiceSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysAtomicServiceRecord::class,

    /** 编码 */
    var code: String? = null,

    /** 名称 */
    var name: String? = null,

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

    constructor() : this(SysAtomicServiceRecord::class)

    //endregion your codes 3

}