package io.kudos.ms.sys.common.vo.cache

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 缓存缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheCacheItem (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

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

    /** 是否为 Hash 缓存 */
    var hash: Boolean,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

//    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 7167286658481070161L
    }

}