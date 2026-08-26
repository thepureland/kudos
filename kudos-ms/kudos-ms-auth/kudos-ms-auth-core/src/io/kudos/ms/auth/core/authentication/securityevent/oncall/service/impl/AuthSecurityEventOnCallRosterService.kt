package io.kudos.ms.auth.core.authentication.securityevent.oncall.service.impl

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRoster
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterAuditRecord
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShift
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShiftCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.cache.AuthSecurityEventOnCallRosterCache
import io.kudos.ms.auth.core.authentication.securityevent.oncall.dao.AuthSecurityEventOnCallRosterDao
import io.kudos.ms.auth.core.authentication.securityevent.oncall.dao.AuthSecurityEventOnCallShiftDao
import io.kudos.ms.auth.core.authentication.securityevent.oncall.event.AuthSecurityEventOnCallRosterChanged
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po.AuthSecurityEventOnCallRosterPo
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po.AuthSecurityEventOnCallShiftPo
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Persisted on-call rotations.
 *
 * The tenant and the operator always come from the caller's trusted administrator context; the stored command
 * is never consulted for either.
 */
@Service
open class AuthSecurityEventOnCallRosterService(
    private val rosterDao: AuthSecurityEventOnCallRosterDao,
    private val shiftDao: AuthSecurityEventOnCallShiftDao,
    private val cache: AuthSecurityEventOnCallRosterCache,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock = Clock.systemUTC(),
) : IAuthSecurityEventOnCallRosterService {

    @Transactional(readOnly = true)
    override fun listByTenant(tenantId: String): List<AuthSecurityEventOnCallRoster> {
        requireIdentifier(tenantId, TENANT_INVALID)
        val shiftsByRoster = shiftDao.findByTenant(tenantId).groupBy { it.rosterId }
        return rosterDao.findByTenant(tenantId).map { it.toRoster(shiftsByRoster[it.id].orEmpty()) }
    }

    @Transactional(readOnly = true)
    override fun findEnabled(tenantId: String, rosterCode: String): AuthSecurityEventOnCallRoster? {
        requireIdentifier(tenantId, TENANT_INVALID)
        val code = rosterCode.trim()
        if (!ROSTER_CODE.matches(code)) fail(ROSTER_CODE_INVALID)
        return cache.getRosters(tenantId).singleOrNull { it.rosterCode == code && it.enabled }
    }

    @Transactional
    override fun save(command: AuthSecurityEventOnCallRosterSaveCommand): AuthSecurityEventOnCallRoster {
        val validated = validate(command)
        val existing = rosterDao.findByCode(validated.tenantId, validated.rosterCode)
        val before = existing?.let { it.toRoster(shiftDao.findByTenant(validated.tenantId).filter { s -> s.rosterId == it.id }) }
        val now = LocalDateTime.now(clock)
        val roster = if (existing == null) insert(validated, now) else update(existing, before!!, validated, now)
        shiftDao.deleteByRoster(validated.tenantId, roster.id)
        val shifts = validated.shifts.map { shift ->
            AuthSecurityEventOnCallShiftPo {
                id = UUID.randomUUID().toString()
                tenantId = validated.tenantId
                rosterId = roster.id
                responderUserId = shift.responderUserId
                tier = shift.tier
                startAt = shift.startAt
                endAt = shift.endAt
            }.also(shiftDao::insert)
        }
        val saved = roster.toRoster(shifts)
        rosterDao.insertChangeAudit(
            AuthSecurityEventOnCallRosterAuditRecord(
                id = UUID.randomUUID().toString(),
                tenantId = validated.tenantId,
                rosterId = roster.id,
                actorUserId = validated.actorUserId,
                reason = validated.reason,
                configVersion = saved.configVersion,
                beforeSnapshot = before?.let(::snapshot),
                afterSnapshot = snapshot(saved),
                changedAt = now,
            )
        )
        eventPublisher.publishEvent(AuthSecurityEventOnCallRosterChanged(validated.tenantId))
        return saved
    }

    private fun insert(
        validated: AuthSecurityEventOnCallRosterSaveCommand,
        now: LocalDateTime,
    ): AuthSecurityEventOnCallRosterPo {
        if (validated.expectedVersion != 0L) fail(VERSION_CONFLICT)
        val roster = AuthSecurityEventOnCallRosterPo {
            id = UUID.randomUUID().toString()
            tenantId = validated.tenantId
            rosterCode = validated.rosterCode
            displayName = validated.displayName
            enabled = validated.enabled
            configVersion = FIRST_VERSION
            createUserId = validated.actorUserId
            createReason = validated.reason
            createTime = now
            updateUserId = validated.actorUserId
            updateReason = validated.reason
            updateTime = now
        }
        try {
            rosterDao.insert(roster)
        } catch (e: DataIntegrityViolationException) {
            // Another administrator created the same code first; their version is now the one to build on.
            throw AuthSecurityEventException(VERSION_CONFLICT, e)
        }
        return roster
    }

    private fun update(
        existing: AuthSecurityEventOnCallRosterPo,
        before: AuthSecurityEventOnCallRoster,
        validated: AuthSecurityEventOnCallRosterSaveCommand,
        now: LocalDateTime,
    ): AuthSecurityEventOnCallRosterPo {
        if (before.configVersion != validated.expectedVersion) fail(VERSION_CONFLICT)
        existing.displayName = validated.displayName
        existing.enabled = validated.enabled
        existing.updateUserId = validated.actorUserId
        existing.updateReason = validated.reason
        existing.updateTime = now
        if (!rosterDao.updateWithVersion(existing, validated.expectedVersion)) fail(VERSION_CONFLICT)
        existing.configVersion = validated.expectedVersion + 1
        return existing
    }

    private fun AuthSecurityEventOnCallRosterPo.toRoster(shifts: List<AuthSecurityEventOnCallShiftPo>) =
        AuthSecurityEventOnCallRoster(
            tenantId = tenantId,
            rosterCode = rosterCode,
            displayName = displayName,
            enabled = enabled,
            shifts = shifts.sortedWith(compareBy({ it.startAt }, { it.tier }, { it.responderUserId })).map {
                AuthSecurityEventOnCallShift(
                    id = it.id,
                    responderUserId = it.responderUserId,
                    tier = it.tier,
                    startAt = it.startAt,
                    endAt = it.endAt,
                )
            },
            configVersion = configVersion,
            createUserId = createUserId,
            createReason = createReason,
            createTime = createTime,
            updateUserId = updateUserId,
            updateReason = updateReason,
            updateTime = updateTime,
        )

    /** Stable, orderable change evidence so a replaced rotation can be read back line by line. */
    private fun snapshot(roster: AuthSecurityEventOnCallRoster): String = buildString {
        append("displayName=").append(roster.displayName)
        append(";enabled=").append(roster.enabled)
        append(";shifts=")
        append(
            roster.shifts
                .map { "${it.responderUserId}@${it.tier}:${it.startAt}/${it.endAt}" }
                .sorted()
                .joinToString(",")
        )
    }.take(MAX_SNAPSHOT_LENGTH)

    private fun validate(
        command: AuthSecurityEventOnCallRosterSaveCommand,
    ): AuthSecurityEventOnCallRosterSaveCommand {
        requireIdentifier(command.tenantId, TENANT_INVALID)
        requireIdentifier(command.actorUserId, ACTOR_INVALID)
        val reason = command.reason.trim()
        if (reason.isBlank() || reason.length > MAX_REASON_LENGTH || reason.any(Char::isISOControl)) {
            fail(REASON_INVALID)
        }
        if (command.expectedVersion < 0) fail(VERSION_INVALID)
        val rosterCode = command.rosterCode.trim()
        if (!ROSTER_CODE.matches(rosterCode)) fail(ROSTER_CODE_INVALID)
        val displayName = command.displayName.trim()
        if (displayName.isBlank() || displayName.length > MAX_NAME_LENGTH || displayName.any(Char::isISOControl)) {
            fail(NAME_INVALID)
        }
        if (command.shifts.size > MAX_SHIFTS) fail(SHIFTS_TOO_MANY)
        val shifts = command.shifts.map { shift ->
            val responderUserId = shift.responderUserId.trim()
            if (responderUserId.isBlank() || responderUserId.length > MAX_IDENTIFIER_LENGTH ||
                responderUserId.any(Char::isISOControl)
            ) {
                fail(SHIFT_RESPONDER_INVALID)
            }
            if (shift.tier !in MIN_TIER..MAX_TIER) fail(SHIFT_TIER_INVALID)
            if (!shift.startAt.isBefore(shift.endAt)) fail(SHIFT_WINDOW_INVALID)
            AuthSecurityEventOnCallShiftCommand(responderUserId, shift.tier, shift.startAt, shift.endAt)
        }
        // Two identical windows for the same responder and tier are a duplicated paste, not a rotation.
        if (shifts.distinctBy { listOf(it.responderUserId, it.tier, it.startAt, it.endAt) }.size != shifts.size) {
            fail(SHIFT_DUPLICATED)
        }
        return command.copy(
            rosterCode = rosterCode,
            displayName = displayName,
            shifts = shifts,
            reason = reason,
        )
    }

    private fun requireIdentifier(value: String, errorCode: String) {
        if (value.isBlank() || value.length > MAX_IDENTIFIER_LENGTH || value.any(Char::isISOControl)) {
            fail(errorCode)
        }
    }

    private fun fail(errorCode: String): Nothing = throw AuthSecurityEventException(errorCode)

    internal companion object {
        const val MIN_TIER = 1
        const val MAX_TIER = 5
        const val MAX_SHIFTS = 100
        const val FIRST_VERSION = 1L
        const val MAX_REASON_LENGTH = 512
        const val MAX_NAME_LENGTH = 128
        const val MAX_IDENTIFIER_LENGTH = 36
        const val MAX_SNAPSHOT_LENGTH = 8192
        const val TENANT_INVALID = "AUTH_SECURITY_EVENT_ONCALL_TENANT_INVALID"
        const val ACTOR_INVALID = "AUTH_SECURITY_EVENT_ONCALL_ACTOR_INVALID"
        const val REASON_INVALID = "AUTH_SECURITY_EVENT_ONCALL_REASON_INVALID"
        const val VERSION_INVALID = "AUTH_SECURITY_EVENT_ONCALL_VERSION_INVALID"
        const val VERSION_CONFLICT = "AUTH_SECURITY_EVENT_ONCALL_VERSION_CONFLICT"
        const val ROSTER_CODE_INVALID = "AUTH_SECURITY_EVENT_ONCALL_ROSTER_CODE_INVALID"
        const val NAME_INVALID = "AUTH_SECURITY_EVENT_ONCALL_ROSTER_NAME_INVALID"
        const val SHIFTS_TOO_MANY = "AUTH_SECURITY_EVENT_ONCALL_SHIFTS_TOO_MANY"
        const val SHIFT_RESPONDER_INVALID = "AUTH_SECURITY_EVENT_ONCALL_SHIFT_RESPONDER_INVALID"
        const val SHIFT_TIER_INVALID = "AUTH_SECURITY_EVENT_ONCALL_SHIFT_TIER_INVALID"
        const val SHIFT_WINDOW_INVALID = "AUTH_SECURITY_EVENT_ONCALL_SHIFT_WINDOW_INVALID"
        const val SHIFT_DUPLICATED = "AUTH_SECURITY_EVENT_ONCALL_SHIFT_DUPLICATED"
        val ROSTER_CODE = Regex("^[A-Z0-9_.:-]{1,64}$")
    }
}
