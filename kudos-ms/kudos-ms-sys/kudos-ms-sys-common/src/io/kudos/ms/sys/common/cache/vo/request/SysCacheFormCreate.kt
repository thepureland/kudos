package io.kudos.ms.sys.common.cache.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * Cache form create request VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheFormCreate (

    /** Name */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    @get:MaxLength(64)
    val name: String ,

    /** Atomic service code */
    @get:NotBlank
    @get:MaxLength(32)
    val atomicServiceCode: String ,

    override val strategyDictCode: String ,

    override val writeOnBoot: Boolean ,

    override val writeInTime: Boolean ,

    override val ttl: Int? ,

    override val remark: String? ,

    /** Whether it is a Hash cache */
    val hash: Boolean ,

) : ISysCacheFormBase