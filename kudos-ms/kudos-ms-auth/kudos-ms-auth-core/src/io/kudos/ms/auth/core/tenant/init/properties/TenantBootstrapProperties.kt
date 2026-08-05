package io.kudos.ms.auth.core.tenant.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


/**
 * What a newly created tenant is seeded with.
 *
 * **The question this answers.** Every authorization model has a bootstrap problem: permissions come
 * from roles, roles are administered by somebody who holds a role, and a brand-new tenant has
 * neither. Left unanswered, each deployment solves it the same bad way — a hand-written INSERT, a
 * hardcoded "if tenantId is new then allow", a shared platform account handed to the customer. All
 * three are unauditable, and the last one is a cross-tenant credential.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "kudos.ms.auth.tenant-bootstrap")
open class TenantBootstrapProperties {

    /**
     * Whether a new tenant is seeded automatically when `sys_tenant` gains a row.
     *
     * On by default: a tenant with no roles cannot be administered by anyone in it, so the useful
     * default state is "seeded". Seeding creates roles, which grant nobody anything until somebody
     * is bound to one — the act that actually confers access stays explicit.
     */
    var enabled: Boolean = true

    /** The roles every new tenant starts with. */
    var roles: List<BootstrapRole> = listOf(BootstrapRole())

    /**
     * One seeded role.
     *
     * @author K
     * @author AI: Claude
     * @since 1.0.0
     */
    open class BootstrapRole {

        /** Role code, unique within the tenant. */
        var code: String = "tenant-admin"

        /** Display name. */
        var name: String = "租户管理员"

        /** Subsystem the role belongs to. */
        var subsysCode: String = "ams"

        /**
         * The permission codes the role is granted.
         *
         * Deliberately a named list rather than `*`. A wildcard here would read as "tenant
         * administrator", but the decision algebra matches codes without regard to tenancy — so `*`
         * grants *every action in the deployment*, and relies entirely on the data layer to keep the
         * blast radius inside one tenant. That is a lot of trust to place in a default. The listed
         * codes are the administrative surface a tenant genuinely needs to run itself; a deployment
         * that wants more says so.
         */
        var permissionCodes: List<String> = listOf(
            "auth:role:*",
            "auth:group:*",
            "user:account:*",
        )

        /**
         * How far the role may be delegated onward. Two hops by default: the first administrator
         * needs to be able to appoint others, and those others should not be able to spread it
         * without limit.
         */
        var delegableMax: Int = 2
    }
}
