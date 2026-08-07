package io.kudos.ms.auth.core.platform.authz.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


/**
 * Tuning for the authorization decision point.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "kudos.ms.auth.authz")
open class AuthzProperties {

    /**
     * Role codes whose holders short-circuit the decision algebra to PERMIT.
     *
     * Empty by default — a framework that ships a magic role enabled out of the box ships a
     * backdoor. A deployment opts in by naming its own bootstrap role here, and every use is still
     * recorded with `Reason.PLATFORM_ADMIN` rather than looking like an ordinary grant.
     *
     * These roles must never be delegable (`auth_role.delegable_max = 0`), or the short circuit
     * becomes something an administrator can hand out.
     */
    var platformAdminRoleCodes: Set<String> = emptySet()

    /**
     * Tenant ids in which a platform-administrator role is allowed to exist.
     *
     * A role code is tenant-scoped, so matching a code alone lets any tenant create a homonym and
     * acquire the short circuit. Both this set and [platformAdminRoleCodes] must be configured.
     */
    var platformAdminTenantIds: Set<String> = emptySet()

    /** Emit an audit record for every decision; normally DENY and platform bypasses are sufficient. */
    var auditAllDecisions: Boolean = false

    /** Audit denied decisions by default, without requiring every caller to remember to do it. */
    var auditDeniedDecisions: Boolean = true

    /** Platform-administrator short circuits are always security-significant. */
    var auditPlatformAdminDecisions: Boolean = true

    /** When true, an audit sink failure aborts the request instead of degrading to an error log. */
    var failOnAuditError: Boolean = false
}
