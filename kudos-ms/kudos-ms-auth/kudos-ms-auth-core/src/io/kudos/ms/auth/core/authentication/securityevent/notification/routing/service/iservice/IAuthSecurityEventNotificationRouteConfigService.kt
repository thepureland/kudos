package io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAppliesToEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteConfig
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteSaveCommand

/** Tenant-scoped notification routing configuration: authoritative reads for management, cached reads for delivery. */
interface IAuthSecurityEventNotificationRouteConfigService {

    /** Every stored rule of the tenant, read straight from the database so management always sees the truth. */
    fun listByTenant(tenantId: String): List<AuthSecurityEventNotificationRouteConfig>

    /**
     * The enabled rule for one delivery shape, or `null` when the tenant configured nothing usable and the
     * built-in default applies. Served from the cross-node cache: it is on the delivery path.
     */
    fun findEffective(
        tenantId: String,
        notificationType: AuthSecurityEventNotificationTypeEnum,
        appliesTo: AuthSecurityEventNotificationRouteAppliesToEnum,
    ): AuthSecurityEventNotificationRouteConfig?

    /** Upserts exactly one rule under optimistic concurrency control, with an appended change audit. */
    fun save(command: AuthSecurityEventNotificationRouteSaveCommand): AuthSecurityEventNotificationRouteConfig
}
