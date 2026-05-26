package io.kudos.ms.sys.common.datasource.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank


/**
 * Data source connectivity test request
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceTestRequest(

    /** JDBC URL */
    @get:NotBlank
    @get:MaxLength(256)
    val url: String,

    /** Username */
    @get:NotBlank
    @get:MaxLength(32)
    val username: String,

    /** Password (plaintext; caller is responsible for secure transport) */
    val password: String?,

)
