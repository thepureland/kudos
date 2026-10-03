package io.kudos.ms.auth.core.organization.init

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Organization-mode authorization settings.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ms.auth.organization")
open class OrganizationAuthorizationProperties {

    /**
     * Permission codes of organization management: what organization administrators and the holders of
     * the two management roles may call in the organization scope (and organization administrators also
     * inside a tenant). Fine-grained rules (who may change what) are enforced by the services behind them.
     */
    var managementPermissionCodes: List<String> = listOf(
        "user:account:*",
        "user:org:*",
        "user:accountThird:*",
        "user:contactWay:*",
        "auth:role:*",
        "auth:group:*",
        "auth:roleExclusion:*",
        "auth:roleDataScope:*",
        "auth:resourcepermission:*",
        "auth:organization:*",
        "sys:system:getAllActiveSubSystemCodes",
    )

    /**
     * The sub-system the organization scope works in (the management console). Sessions in the
     * organization scope carry it as their sub-system.
     */
    var managementSubSystemCode: String = "console"
}
