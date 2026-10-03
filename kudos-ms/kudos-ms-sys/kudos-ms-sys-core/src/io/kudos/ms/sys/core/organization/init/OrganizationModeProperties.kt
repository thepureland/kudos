package io.kudos.ms.sys.core.organization.init

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Organization mode switch, shared by sys, user and auth.
 *
 * Off (the default) keeps every tenant on the legacy single-tenant model; on lets one customer
 * organization own several tenants that share people, accounts and roles. The switch is one-way:
 * once a deployment has started in organization mode it refuses to start with the switch off.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ms.organization")
open class OrganizationModeProperties {

    /** Whether organization mode is on. */
    var enabled: Boolean = false

    /**
     * Data source key that holds identity, directory and authorization metadata. When null, the dynamic
     * data source's primary is used. Only consulted in organization mode with dynamic routing.
     */
    var controlPlaneDatasource: String? = null
}
