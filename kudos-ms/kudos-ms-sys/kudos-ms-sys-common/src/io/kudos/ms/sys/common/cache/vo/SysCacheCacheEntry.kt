package io.kudos.ms.sys.common.cache.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Cache-configuration cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheCacheEntry (

    /** Primary key */
    override val id: String,

    /** Name */
    val name: String,

    /** Atomic service code */
    val atomicServiceCode: String,

    /** Cache strategy code */
    val strategyDictCode: String,

    /** Write to cache on boot */
    val writeOnBoot: Boolean,

    /** Write back to cache in real time */
    val writeInTime: Boolean,

    /** Cache time-to-live (seconds) */
    val ttl: Int?,

    /** Remark */
    val remark: String?,

    /** Whether enabled */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

    /** Whether it is a Hash cache */
    val hash: Boolean,

    /** Creator id */
    val createUserId: String?,

    /** Creator name */
    val createUserName: String?,

    /** Create time */
    val createTime: LocalDateTime?,

    /** Updater id */
    val updateUserId: String?,

    /** Updater name */
    val updateUserName: String?,

    /** Update time */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 7167286658481070161L
    }

}
