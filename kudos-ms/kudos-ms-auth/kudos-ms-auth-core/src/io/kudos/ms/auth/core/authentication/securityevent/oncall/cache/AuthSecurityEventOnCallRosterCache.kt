package io.kudos.ms.auth.core.authentication.securityevent.oncall.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRoster
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShift
import io.kudos.ms.auth.core.authentication.securityevent.oncall.dao.AuthSecurityEventOnCallRosterDao
import io.kudos.ms.auth.core.authentication.securityevent.oncall.dao.AuthSecurityEventOnCallShiftDao
import io.kudos.ms.auth.core.authentication.securityevent.oncall.event.AuthSecurityEventOnCallRosterChanged
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * A tenant's rotations and their shifts, cached.
 *
 * **Why the shifts are cached even though "who is on call" changes by the minute.** The cached value is the
 * rotation, which changes only when an administrator edits it; the time filter is applied by the reader against
 * the current instant. Caching the *answer* instead would need an entry per instant and an expiry nobody could
 * choose correctly — caching the *plan* has neither problem, and the delivery path still avoids the database.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class AuthSecurityEventOnCallRosterCache :
    AbstractKeyValueCacheHandler<List<AuthSecurityEventOnCallRoster>>() {

    @Resource
    private lateinit var rosterDao: AuthSecurityEventOnCallRosterDao

    @Resource
    private lateinit var shiftDao: AuthSecurityEventOnCallShiftDao

    companion object {
        private const val CACHE_NAME = "AUTH_SECURITY_EVENT_ONCALL_ROSTER_BY_TENANT_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<AuthSecurityEventOnCallRoster> =
        getSelf<AuthSecurityEventOnCallRosterCache>().getRosters(key)

    /** Lazily populated: only tenants that actually raise an escalation need their rotation resolved. */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        if (clear) clear()
        log.debug("${CACHE_NAME} is lazily populated; nothing pre-loaded.")
    }

    /**
     * Every rotation of the tenant with its shifts attached.
     *
     * @param tenantId the tenant
     * @return the tenant's rotations; empty when none is configured
     */
    @Cacheable(cacheNames = [CACHE_NAME], key = "#tenantId")
    open fun getRosters(tenantId: String): List<AuthSecurityEventOnCallRoster> {
        val shiftsByRoster = shiftDao.findByTenant(tenantId).groupBy { it.rosterId }
        return rosterDao.findByTenant(tenantId).map { roster ->
            AuthSecurityEventOnCallRoster(
                tenantId = roster.tenantId,
                rosterCode = roster.rosterCode,
                displayName = roster.displayName,
                enabled = roster.enabled,
                shifts = shiftsByRoster[roster.id].orEmpty().map { shift ->
                    AuthSecurityEventOnCallShift(
                        id = shift.id,
                        responderUserId = shift.responderUserId,
                        tier = shift.tier,
                        startAt = shift.startAt,
                        endAt = shift.endAt,
                    )
                },
                configVersion = roster.configVersion,
                createUserId = roster.createUserId,
                createReason = roster.createReason,
                createTime = roster.createTime,
                updateUserId = roster.updateUserId,
                updateReason = roster.updateReason,
                updateTime = roster.updateTime,
            )
        }
    }

    /** Invalidates after the change is durable, then lets the cache tier broadcast it to the other nodes. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthSecurityEventOnCallRosterChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, event.tenantId)
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<AuthSecurityEventOnCallRosterCache>().getRosters(event.tenantId)
        }
    }

    private val log = LogFactory.getLog(this::class)
}
