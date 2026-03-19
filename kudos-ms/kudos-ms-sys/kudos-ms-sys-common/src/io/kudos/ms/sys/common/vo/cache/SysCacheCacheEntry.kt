package io.kudos.ms.sys.common.vo.cache

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


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

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 7167286658481070161L
    }

}