package io.kudos.ability.security.enforcement.point

import io.kudos.ability.security.enforcement.point.PermissionPointDefinition.Companion.DEFAULT_SUBSYSTEM
import org.springframework.boot.context.properties.ConfigurationProperties


/**
 * Configuration for permission-point registration.
 *
 * Deliberately a separate prefix from `kudos.ability.security.enforcement`: registration and
 * enforcement are switched independently, and have to be — you register first and enforce second.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.security.point-registration")
open class PermissionPointRegistrationProperties {

    /**
     * Whether points are registered on startup. **On by default**, unlike enforcement: a registered
     * point grants nobody anything, and the alternative default leaves every deployment hand-editing
     * seed SQL before it can use the feature at all.
     */
    var enabled: Boolean = true

    /**
     * Log what would be registered without writing anything.
     *
     * For inspecting what the naming convention makes of an unfamiliar path layout before letting
     * it create rows.
     */
    var dryRun: Boolean = false

    /** Subsystem recorded on auto-registered points. */
    var subsystem: String = DEFAULT_SUBSYSTEM

    /**
     * Path prefixes skipped by the endpoint scan — infrastructure endpoints that are not permission
     * points at all. Matched as plain prefixes, not Ant patterns: this is a coarse exclusion, and
     * anything needing a pattern is better expressed as a public path on the enforcement side.
     */
    var excludedPaths: List<String> = listOf(
        "/actuator",
        "/error",
        "/swagger",
        "/v3/api-docs",
        "/webjars",
    )
}
