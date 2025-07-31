package io.kudos.ams.sys.common.vo.subsystemmicroservice

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 子系统-微服务关系查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysSubSystemMicroServiceSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysSubSystemMicroServiceRecord::class,

    /** 子系统编码 */
    var subSystemCode: String? = null,

    /** 微服务编码 */
    var microServiceCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysSubSystemMicroServiceRecord::class)

    //endregion your codes 3

}