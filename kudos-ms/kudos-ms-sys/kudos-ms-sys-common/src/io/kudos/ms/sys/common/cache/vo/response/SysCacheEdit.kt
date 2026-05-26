package io.kudos.ms.sys.common.cache.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Cache configuration edit response VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheEdit (

    /** Primary key */
    override val id: String = "",

    /** Name */
    val name: String = "",

    /** Atomic service code */
    val atomicServiceCode: String = "",

    /** Cache strategy code */
    val strategyDictCode: String = "",

    /** Write to cache on boot */
    val writeOnBoot: Boolean = true,

    /** Write back to cache in real time */
    val writeInTime: Boolean = true,

    /** Cache TTL (seconds) */
    val ttl: Int? = null,

    /** Remark */
    val remark: String? = null,

    /** Whether it is a Hash cache */
    val hash: Boolean = false,

) : IIdEntity<String>