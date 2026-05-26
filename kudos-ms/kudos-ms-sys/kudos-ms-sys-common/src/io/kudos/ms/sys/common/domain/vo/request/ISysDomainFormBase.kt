package io.kudos.ms.sys.common.domain.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

/**
 * Domain form base fields (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDomainFormBase {

    /** Domain */
    @get:NotBlank
    @get:MaxLength(256)
    @get:Matches(RegExpEnum.DOMAIN)
    val domain: String

    /** System code */
    @get:NotBlank
    @get:MaxLength(32)
    val systemCode: String

    /** Tenant id */
    @get:NotBlank
    val tenantId: String

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
