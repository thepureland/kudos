package io.kudos.ms.sys.common.cache.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Cache detail response VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheDetail (

    /** Primary key */
    override val id: String = "",

    /** Name */
    val name: String = "",

    /** Atomic service code */
    val atomicServiceCode: String = "",

    /** Cache strategy code */
    val strategyDictCode: String = "",

    /** Whether to write cache on startup */
    val writeOnBoot: Boolean = true,

    /** Whether to write back cache in real time */
    val writeInTime: Boolean = true,

    /** Cache time-to-live (seconds) */
    val ttl: Int? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = true,

    /** Whether it is a Hash cache */
    val hash: Boolean = false,

    /** Creator id */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater id */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>