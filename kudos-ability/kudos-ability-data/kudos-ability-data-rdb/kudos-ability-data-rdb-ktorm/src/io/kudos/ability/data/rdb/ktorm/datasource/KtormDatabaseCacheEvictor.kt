package io.kudos.ability.data.rdb.ktorm.datasource

import io.kudos.base.logger.LogFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

/**
 * Drops every cached ktorm `Database` when an application context closes.
 *
 * **Why the weak keys were not enough.** The cache is a `WeakHashMap`, which holds keys weakly but values
 * strongly, and the value — a `Database` built by `connectWithSpringSupport` — captures the *routing*
 * datasource while the key is the datasource that routing resolved to. When nothing is routing, those are the
 * same object, so the value strongly references its own key and the entry can never be collected. When
 * something is routing, the key can be a long-lived inner datasource while the value still points at the
 * routing datasource of whichever context happened to build it first. Either way an entry can outlive the
 * context that owns its connection pool, and the next caller gets a `Database` whose pool is shut down —
 * which surfaces as `ProxyConnection.close()` failing with a null delegate, some distance from the cause.
 *
 * A context closing is the moment its pools go away, so it is also the moment those entries stop being
 * usable. Everything is dropped rather than only this context's entries: the cache key is a resolved inner
 * datasource that need not be a bean of the closing context, so "which entries belong to it" cannot be
 * answered reliably, and rebuilding an entry costs one metadata round trip on the next query.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class KtormDatabaseCacheEvictor : ApplicationListener<ContextClosedEvent> {

    override fun onApplicationEvent(event: ContextClosedEvent) {
        clearKtormDatabaseCache()
        log.debug("Cleared the ktorm Database cache because an application context closed.")
    }

    private val log = LogFactory.getLog(this::class)
}
