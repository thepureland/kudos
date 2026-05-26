package io.kudos.ms.sys.common.tenant.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * Tenant form base fields (shared by create / update).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantFormBase {

    /** Name */
    @get:NotBlank
    @get:MaxLength(128)
    val name: String

    /** Owning subsystems */
    @get:NotEmpty
    var subSystemCodes: Set<String>

    /** Timezone */
    @get:MaxLength(128)
    val timezone: String?

    /** Default language code */
    @get:FixedLength(5)
    val defaultLanguageCode: String?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
