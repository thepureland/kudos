package io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.impl

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAppliesToEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAuditRecord
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteConfig
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteFallbackEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteMapper
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.cache.AuthSecurityEventNotificationRouteCache
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.dao.AuthSecurityEventNotificationRouteDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.event.AuthSecurityEventNotificationRouteChanged
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.model.po.AuthSecurityEventNotificationRoute
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Persisted tenant routing rules.
 *
 * The tenant and the operator are always supplied by the caller from a trusted administrator context; nothing
 * in this service reads them back out of the payload it is asked to store.
 */
@Service
open class AuthSecurityEventNotificationRouteConfigService(
    private val dao: AuthSecurityEventNotificationRouteDao,
    private val cache: AuthSecurityEventNotificationRouteCache,
    private val rosterService: IAuthSecurityEventOnCallRosterService,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock = Clock.systemUTC(),
) : IAuthSecurityEventNotificationRouteConfigService {

    @Transactional(readOnly = true)
    override fun listByTenant(tenantId: String): List<AuthSecurityEventNotificationRouteConfig> {
        requireIdentifier(tenantId, TENANT_INVALID)
        return dao.findByTenant(tenantId).map(AuthSecurityEventNotificationRouteMapper::toConfig)
    }

    @Transactional(readOnly = true)
    override fun findEffective(
        tenantId: String,
        notificationType: AuthSecurityEventNotificationTypeEnum,
        appliesTo: AuthSecurityEventNotificationRouteAppliesToEnum,
    ): AuthSecurityEventNotificationRouteConfig? {
        requireIdentifier(tenantId, TENANT_INVALID)
        return cache.getRoutes(tenantId).singleOrNull {
            it.notificationType == notificationType && it.appliesTo == appliesTo && it.enabled
        }
    }

    @Transactional
    override fun save(
        command: AuthSecurityEventNotificationRouteSaveCommand,
    ): AuthSecurityEventNotificationRouteConfig {
        val validated = validate(command)
        val existing = dao.findByScope(
            command.tenantId,
            validated.notificationType.name,
            validated.appliesTo.name,
        )
        val before = existing?.let(AuthSecurityEventNotificationRouteMapper::toConfig)
        val now = LocalDateTime.now(clock)
        val stored = if (existing == null) insert(validated, now) else update(existing, before!!, validated, now)
        dao.insertChangeAudit(
            AuthSecurityEventNotificationRouteAuditRecord(
                id = UUID.randomUUID().toString(),
                tenantId = validated.tenantId,
                routeId = stored.id,
                actorUserId = validated.actorUserId,
                reason = validated.reason,
                configVersion = stored.configVersion,
                beforeSnapshot = before?.let(AuthSecurityEventNotificationRouteMapper::toSnapshot),
                afterSnapshot = AuthSecurityEventNotificationRouteMapper.toSnapshot(validated.toConfig(stored)),
                changedAt = now,
            )
        )
        eventPublisher.publishEvent(AuthSecurityEventNotificationRouteChanged(validated.tenantId))
        return validated.toConfig(stored)
    }

    private fun insert(
        validated: ValidatedCommand,
        now: LocalDateTime,
    ): AuthSecurityEventNotificationRoute {
        if (validated.expectedVersion != 0L) fail(VERSION_CONFLICT)
        val route = AuthSecurityEventNotificationRoute {
            id = UUID.randomUUID().toString()
            tenantId = validated.tenantId
            notificationType = validated.notificationType.name
            appliesTo = validated.appliesTo.name
            createUserId = validated.actorUserId
            createReason = validated.reason
            createTime = now
            configVersion = FIRST_VERSION
        }
        validated.applyTo(route, now)
        try {
            dao.insert(route)
        } catch (e: DataIntegrityViolationException) {
            // Another administrator created the same scope first; their version is now the one to build on.
            throw AuthSecurityEventException(VERSION_CONFLICT, e)
        }
        return route
    }

    private fun update(
        existing: AuthSecurityEventNotificationRoute,
        before: AuthSecurityEventNotificationRouteConfig,
        validated: ValidatedCommand,
        now: LocalDateTime,
    ): AuthSecurityEventNotificationRoute {
        if (before.configVersion != validated.expectedVersion) fail(VERSION_CONFLICT)
        validated.applyTo(existing, now)
        if (!dao.updateWithVersion(existing, validated.expectedVersion)) fail(VERSION_CONFLICT)
        existing.configVersion = validated.expectedVersion + 1
        return existing
    }

    private fun ValidatedCommand.applyTo(route: AuthSecurityEventNotificationRoute, now: LocalDateTime) {
        route.routeCode = routeCode
        route.destination = destination.name
        route.channels = channels.map { it.name }.sorted().joinToString(",")
        route.responderUserIds = AuthSecurityEventNotificationRouteMapper.csvOrNull(responderUserIds)
        route.responderRosterCode = responderRosterCode
        route.includeAssignee = includeAssignee
        route.enabled = enabled
        route.fallbackBehavior = fallbackBehavior.name
        route.updateUserId = actorUserId
        route.updateReason = reason
        route.updateTime = now
    }

    private fun ValidatedCommand.toConfig(route: AuthSecurityEventNotificationRoute) =
        AuthSecurityEventNotificationRouteConfig(
            tenantId = tenantId,
            notificationType = notificationType,
            appliesTo = appliesTo,
            routeCode = routeCode,
            destination = destination,
            channels = channels,
            responderUserIds = responderUserIds,
            responderRosterCode = responderRosterCode,
            includeAssignee = includeAssignee,
            enabled = enabled,
            fallbackBehavior = fallbackBehavior,
            configVersion = route.configVersion,
            createUserId = route.createUserId,
            createReason = route.createReason,
            createTime = route.createTime,
            updateUserId = route.updateUserId,
            updateReason = route.updateReason,
            updateTime = route.updateTime,
        )

    private fun validate(command: AuthSecurityEventNotificationRouteSaveCommand): ValidatedCommand {
        requireIdentifier(command.tenantId, TENANT_INVALID)
        requireIdentifier(command.actorUserId, ACTOR_INVALID)
        val reason = command.reason.trim()
        if (reason.isBlank() || reason.length > MAX_REASON_LENGTH || reason.any(Char::isISOControl)) {
            fail(REASON_INVALID)
        }
        if (command.expectedVersion < 0) fail(VERSION_INVALID)
        val notificationType = parse(command.notificationType, TYPE_INVALID) {
            AuthSecurityEventNotificationTypeEnum.valueOf(it)
        }
        val appliesTo = parse(command.appliesTo, APPLIES_TO_INVALID) {
            AuthSecurityEventNotificationRouteAppliesToEnum.valueOf(it)
        }
        val destination = parse(command.destination, DESTINATION_INVALID) {
            AuthSecurityEventNotificationDestinationEnum.valueOf(it)
        }
        val fallbackBehavior = parse(command.fallbackBehavior, FALLBACK_INVALID) {
            AuthSecurityEventNotificationRouteFallbackEnum.valueOf(it)
        }
        // The same shape the dispatcher enforces before publishing, so a rule that saves can never be
        // rejected as malformed at delivery time.
        val routeCode = command.routeCode.trim()
        if (!ROUTE_CODE.matches(routeCode)) fail(ROUTE_CODE_INVALID)
        if (command.channels.isEmpty() || command.channels.size > AuthSecurityEventNotificationChannelEnum.entries.size) {
            fail(CHANNELS_INVALID)
        }
        val channels = command.channels.map { channel ->
            parse(channel, CHANNELS_INVALID) { AuthSecurityEventNotificationChannelEnum.valueOf(it) }
        }.toSet()
        if (command.responderUserIds.size > MAX_RESPONDERS) fail(RESPONDERS_INVALID)
        val responderUserIds = command.responderUserIds.map { it.trim() }.toSortedSet()
        if (responderUserIds.any { it.isBlank() || it.length > MAX_IDENTIFIER_LENGTH || it.any(Char::isISOControl) }) {
            fail(RESPONDERS_INVALID)
        }
        val responderRosterCode = command.responderRosterCode?.trim()?.takeIf { it.isNotEmpty() }?.also {
            if (!ROUTE_CODE.matches(it)) fail(ROSTER_CODE_INVALID)
            if (rosterService.findEnabled(command.tenantId, it) == null) fail(ROSTER_NOT_FOUND)
        }
        // An unassigned escalation has no assignee to include, and a USER route with nobody to address would
        // spend every attempt in the fallback branch: refuse both at the point where somebody can still fix them.
        // A rotation counts as an addressable source even when it is empty right now — that is what a rotation is.
        val addressable = responderUserIds.isNotEmpty() || responderRosterCode != null
        if (appliesTo == AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED) {
            if (command.includeAssignee) fail(INCLUDE_ASSIGNEE_INVALID)
            if (destination == AuthSecurityEventNotificationDestinationEnum.USER && !addressable) {
                fail(RESPONDERS_REQUIRED)
            }
        }
        if (appliesTo == AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED &&
            destination == AuthSecurityEventNotificationDestinationEnum.USER &&
            !command.includeAssignee && !addressable
        ) {
            fail(RESPONDERS_REQUIRED)
        }
        return ValidatedCommand(
            tenantId = command.tenantId,
            notificationType = notificationType,
            appliesTo = appliesTo,
            routeCode = routeCode,
            destination = destination,
            channels = channels,
            responderUserIds = responderUserIds,
            responderRosterCode = responderRosterCode,
            includeAssignee = command.includeAssignee,
            enabled = command.enabled,
            fallbackBehavior = fallbackBehavior,
            expectedVersion = command.expectedVersion,
            actorUserId = command.actorUserId,
            reason = reason,
        )
    }

    private fun <T> parse(value: String, errorCode: String, converter: (String) -> T): T =
        runCatching { converter(value.trim().uppercase()) }.getOrElse { fail(errorCode, it) }

    private fun requireIdentifier(value: String, errorCode: String) {
        if (value.isBlank() || value.length > MAX_IDENTIFIER_LENGTH || value.any(Char::isISOControl)) {
            fail(errorCode)
        }
    }

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw AuthSecurityEventException(errorCode, cause)

    private data class ValidatedCommand(
        val tenantId: String,
        val notificationType: AuthSecurityEventNotificationTypeEnum,
        val appliesTo: AuthSecurityEventNotificationRouteAppliesToEnum,
        val routeCode: String,
        val destination: AuthSecurityEventNotificationDestinationEnum,
        val channels: Set<AuthSecurityEventNotificationChannelEnum>,
        val responderUserIds: Set<String>,
        val responderRosterCode: String?,
        val includeAssignee: Boolean,
        val enabled: Boolean,
        val fallbackBehavior: AuthSecurityEventNotificationRouteFallbackEnum,
        val expectedVersion: Long,
        val actorUserId: String,
        val reason: String,
    )

    private companion object {
        const val FIRST_VERSION = 1L
        const val MAX_REASON_LENGTH = 512
        const val MAX_IDENTIFIER_LENGTH = 36
        const val MAX_RESPONDERS = 50
        const val TENANT_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_TENANT_INVALID"
        const val ACTOR_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_ACTOR_INVALID"
        const val REASON_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_REASON_INVALID"
        const val VERSION_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_INVALID"
        const val VERSION_CONFLICT = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_CONFLICT"
        const val TYPE_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_TYPE_INVALID"
        const val APPLIES_TO_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_APPLIES_TO_INVALID"
        const val DESTINATION_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_DESTINATION_INVALID"
        const val FALLBACK_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_FALLBACK_INVALID"
        const val ROUTE_CODE_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_CODE_INVALID"
        const val CHANNELS_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_CHANNEL_INVALID"
        const val RESPONDERS_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_RESPONDERS_INVALID"
        const val RESPONDERS_REQUIRED = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_RESPONDERS_REQUIRED"
        const val INCLUDE_ASSIGNEE_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_INCLUDE_ASSIGNEE_INVALID"
        const val ROSTER_CODE_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_ROSTER_CODE_INVALID"
        const val ROSTER_NOT_FOUND = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_ROSTER_NOT_FOUND"
        val ROUTE_CODE = Regex("^[A-Z0-9_.:-]{1,64}$")
    }
}
