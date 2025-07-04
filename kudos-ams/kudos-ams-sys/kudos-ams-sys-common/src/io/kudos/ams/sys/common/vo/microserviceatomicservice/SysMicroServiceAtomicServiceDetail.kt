package io.kudos.ams.sys.common.vo.microserviceatomicservice

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 微服务-原子服务关系查询记录
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysMicroServiceAtomicServiceDetail : IdJsonResult<String>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2


    /** 微服务编码 */
    var microServiceCode: String? = null

    /** 原子服务编码 */
    var atomicServiceCode: String? = null

    /** 创建用户 */
    var createUser: String? = null

    /** 创建时间 */
    var createTime: LocalDateTime? = null

    /** 更新用户 */
    var updateUser: String? = null

    /** 更新时间 */
    var updateTime: LocalDateTime? = null

}