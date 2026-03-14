package io.kudos.ms.sys.common.vo.cache

import io.kudos.base.support.IIdEntity
import java.io.Serializable


/**
 * 缓存缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 名称 */
    val name: String = "",

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 缓存策略代码 */
    val strategyDictCode: String = "",

    /** 是否启动时写缓存 */
    val writeOnBoot: Boolean = true,

    /** 是否及时回写缓存 */
    val writeInTime: Boolean = true,

    /** 缓存生存时间(秒) */
    val ttl: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

    /** 是否为 Hash 缓存 */
    val hash: Boolean = false,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 7167286658481070161L
    }

}