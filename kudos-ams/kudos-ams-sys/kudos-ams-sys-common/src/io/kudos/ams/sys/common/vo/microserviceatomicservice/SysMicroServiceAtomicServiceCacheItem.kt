package io.kudos.ams.sys.common.vo.microserviceatomicservice

import java.io.Serializable
import io.kudos.base.support.IIdEntity
import java.time.LocalDateTime


/**
 * 微服务-原子服务关系缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceAtomicServiceCacheItem (

    /**  */
    override var id: String? = null,

    //region your codes 1

    /** 微服务编码 */
    var microServiceCode: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 创建用户 */
    var createUser: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新用户 */
    var updateUser: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 7181450510717295942L
    }

}