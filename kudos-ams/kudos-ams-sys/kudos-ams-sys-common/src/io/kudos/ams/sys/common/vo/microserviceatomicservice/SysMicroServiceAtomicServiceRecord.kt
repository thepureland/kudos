package io.kudos.ams.sys.common.vo.microserviceatomicservice

import io.kudos.base.support.result.IdJsonResult


/**
 * 微服务-原子服务关系查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceAtomicServiceRecord (

    /**  */
    override var id: String? = null,

    //region your codes 1

    /** 微服务编码 */
    var microServiceCode: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}