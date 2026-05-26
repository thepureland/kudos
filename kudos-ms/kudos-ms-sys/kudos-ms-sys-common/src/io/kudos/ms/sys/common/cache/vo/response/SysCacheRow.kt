package io.kudos.ms.sys.common.cache.vo.response

/**
 * Cache list query result response VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheRow (

    /** Primary key */
    val id: String = "",

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

)