package io.kudos.ability.web.swagger.init.properties

/**
 * Author/maintainer contact metadata for the generated OpenAPI document.
 *
 * Mirrors [io.swagger.v3.oas.models.info.Contact] but lives in the kudos property namespace so apps
 * can populate it from yml without a Java config class.
 *
 * Ported from soul's `SwaggerContact`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class SwaggerContact {

    /** Display name of the API maintainer (person or team). */
    var contactName: String? = null

    /** Project / docs / source-of-truth URL — appears as a hyperlink in the UI. */
    var contactUrl: String? = null

    /** Contact email. */
    var contactEmail: String? = null
}
