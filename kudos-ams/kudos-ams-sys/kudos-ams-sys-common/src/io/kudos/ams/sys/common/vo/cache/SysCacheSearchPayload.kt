package io.kudos.ams.sys.common.vo.cache

import io.kudos.base.support.payload.ListSearchPayload
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
    var strategyDictCode: String? = null,

    /** 是否启动时写缓存 */
    var writeOnBoot: Boolean? = null,

    /** 是否及时回写缓存 */
    var writeInTime: Boolean? = null,

    /** 缓存生存时间(秒) */
    var ttl: Int? = null,

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

    constructor() : this(SysCacheRecord::class)

    //endregion your codes 3

}