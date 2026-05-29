package io.kudos.ability.web.swagger.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties bound from `kudos.ability.web.swagger.*` in `application.yml`.
 *
 * Drives the [io.swagger.v3.oas.models.OpenAPI] bean published by
 * [io.kudos.ability.web.swagger.init.SwaggerAutoConfiguration]. Unspecified fields fall back to
 * module-default values supplied by the bundled `kudos-ability-web-swagger.yml`.
 *
 * Ported from soul's `SwaggerProperties`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.web.swagger")
class SwaggerProperties {

    /**
     * Master switch. When false, [SwaggerAutoConfiguration] does not register the OpenAPI bean —
     * use this to turn docs off in a specific profile without excluding the module dependency.
     */
    var enabled: Boolean = true

    /**
     * Hint that the deployment is a production node. Apps can flip this in prod profiles so
     * downstream filters (e.g. an HTTP gate over `/v3/api-docs`) can short-circuit without also
     * stopping OpenAPI generation that build-time SDK codegen still needs.
     */
    var production: Boolean = false

    /** Document title — appears in the rendered Swagger UI header. */
    var title: String? = null

    /** Long-form description of the API surface. */
    var description: String? = null

    /** External terms-of-service or marketing URL displayed alongside the title. */
    var url: String? = null

    /** API version string (e.g. `1.0.0`). */
    var version: String? = null

    /**
     * Group name placeholder reserved for future [org.springdoc.core.models.GroupedOpenApi]
     * support. Soul exposed this field but never wired a [GroupedOpenApi] bean either — kept here
     * for forward compatibility so existing soul yml configs migrate cleanly.
     */
    var groupName: String? = null

    /** Maintainer contact info embedded in the document's `info` block. Defaults to all-null. */
    var contact: SwaggerContact = SwaggerContact()
}
