package io.kudos.ms.sys.common.outline.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

/**
 * Base fields shared by outbound whitelist forms (create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysOutLineFormBase {

    /** Name */
    @get:NotBlank
    @get:MaxLength(64)
    val name: String

    /** Hostname or wildcard */
    @get:NotBlank
    @get:MaxLength(256)
    val host: String

    /** Port; null means any port */
    @get:Min(1)
    @get:Max(65535)
    val port: Int?

    /** Protocol (http/https/tcp/any) */
    @get:NotBlank
    @get:MaxLength(16)
    val protocol: String

    /** System code */
    @get:NotBlank
    @get:MaxLength(32)
    val systemCode: String

    /** Tenant id; null means platform-level */
    val tenantId: String?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
