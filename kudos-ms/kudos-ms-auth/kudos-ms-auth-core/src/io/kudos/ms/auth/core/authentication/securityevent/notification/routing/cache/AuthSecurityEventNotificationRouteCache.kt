package io.kudos.ms.auth.core.authentication.securityevent.notification.routing.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteConfig
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteMapper
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.dao.AuthSecurityEventNotificationRouteDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.event.AuthSecurityEventNotificationRouteChanged
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * A tenant's whole notification routing rule set, cached.
 *
 * **Why the tenant is the key rather than the individual rule.** The rule set is bounded by notification type ×
 * delivery shape, so one entry holds all of it; that also makes invalidation exact — a save touches one tenant,
 * and one evict covers every rule that save could have changed, on this node and (through the LOCAL_REMOTE
 * strategy's broadcast) on every other one.
 *
 * **Why the empty result is cached too.** Tenants that never configured a route are the common case and they
 * are on the delivery path of every escalation attempt; excluding them would mean the dispatcher queries the
 * database once per attempt for exactly the tenants that have nothing to say.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class AuthSecurityEventNotificationRouteCache :
    AbstractKeyValueCacheHandler<List<AuthSecurityEventNotificationRouteConfig>>() {

    @Resource
    private lateinit var dao: AuthSecurityEventNotificationRouteDao

    companion object {
        private const val CACHE_NAME = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_BY_TENANT_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<AuthSecurityEventNotificationRouteConfig> =
        getSelf<AuthSecurityEventNotificationRouteCache>().getRoutes(key)

    /**
     * Lazily populated: the key space is the tenant population, and only tenants that actually raise an
     * escalation ever need their rules resolved.
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        if (clear) clear()
        log.debug("${CACHE_NAME} is lazily populated; nothing pre-loaded.")
    }

    /**
     * Every stored rule for the tenant, enabled or not.
     *
     * @param tenantId the tenant
     * @return the tenant's rules; empty when nothing is configured
     */
    @Cacheable(cacheNames = [CACHE_NAME], key = "#tenantId")
    open fun getRoutes(tenantId: String): List<AuthSecurityEventNotificationRouteConfig> =
        dao.findByTenant(tenantId).map(AuthSecurityEventNotificationRouteMapper::toConfig)

    /**
     * Invalidates after the change is durable.
     *
     * Deliberately after commit: an evict published from inside the transaction would let a concurrent read
     * repopulate the entry from the pre-commit state and leave it stale until the next write.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthSecurityEventNotificationRouteChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, event.tenantId)
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<AuthSecurityEventNotificationRouteCache>().getRoutes(event.tenantId)
        }
    }

    private val log = LogFactory.getLog(this::class)
}
