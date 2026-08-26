package io.kudos.ms.auth.core.authentication.securityevent.notification.routing

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.model.po.AuthSecurityEventNotificationRoute

/**
 * Row-to-rule conversion shared by the read cache and the management service.
 *
 * A stored value that no longer parses is an error rather than a silently dropped rule: dropping it would
 * quietly re-route a tenant's security alerts back to the built-in default without anyone being told.
 */
internal object AuthSecurityEventNotificationRouteMapper {

    fun toConfig(route: AuthSecurityEventNotificationRoute): AuthSecurityEventNotificationRouteConfig =
        AuthSecurityEventNotificationRouteConfig(
            tenantId = route.tenantId,
            notificationType = parse(route.notificationType, "AUTH_SECURITY_EVENT_NOTIFICATION_TYPE_INVALID") {
                AuthSecurityEventNotificationTypeEnum.valueOf(it)
            },
            appliesTo = parse(route.appliesTo, "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_APPLIES_TO_INVALID") {
                AuthSecurityEventNotificationRouteAppliesToEnum.valueOf(it)
            },
            routeCode = route.routeCode,
            destination = parse(route.destination, "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_DESTINATION_INVALID") {
                AuthSecurityEventNotificationDestinationEnum.valueOf(it)
            },
            channels = csvValues(route.channels).map { channel ->
                parse(channel, "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_CHANNEL_INVALID") {
                    AuthSecurityEventNotificationChannelEnum.valueOf(it)
                }
            }.toSet(),
            responderUserIds = csvValues(route.responderUserIds),
            responderRosterCode = route.responderRosterCode,
            includeAssignee = route.includeAssignee,
            enabled = route.enabled,
            fallbackBehavior = parse(
                route.fallbackBehavior,
                "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_FALLBACK_INVALID",
            ) { AuthSecurityEventNotificationRouteFallbackEnum.valueOf(it) },
            configVersion = route.configVersion,
            createUserId = route.createUserId,
            createReason = route.createReason,
            createTime = route.createTime,
            updateUserId = route.updateUserId,
            updateReason = route.updateReason,
            updateTime = route.updateTime,
        )

    /** Stable, human-readable change evidence; ordered so two versions can be diffed by eye. */
    fun toSnapshot(config: AuthSecurityEventNotificationRouteConfig): String = listOf(
        "routeCode=${config.routeCode}",
        "destination=${config.destination.name}",
        "channels=${config.channels.map { it.name }.sorted().joinToString(",")}",
        "responderUserIds=${config.responderUserIds.sorted().joinToString(",")}",
        "responderRosterCode=${config.responderRosterCode ?: ""}",
        "includeAssignee=${config.includeAssignee}",
        "enabled=${config.enabled}",
        "fallbackBehavior=${config.fallbackBehavior.name}",
    ).joinToString(";")

    fun csvOrNull(values: Set<String>): String? = values.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")

    fun csvValues(value: String?): Set<String> =
        value?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() }?.toSortedSet() ?: emptySet()

    private fun <T> parse(value: String, errorCode: String, converter: (String) -> T): T =
        runCatching { converter(value.trim().uppercase()) }
            .getOrElse { throw AuthSecurityEventException(errorCode, it) }
}
