package io.kudos.ability.data.rdb.jdbc.init

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.InitializingBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Configuration-property bean for "package path -> data source key", corresponding
 * to `kudos.ability.jdbc.*` in yml.
 *
 * How it works:
 *  - [packageDataSource]: yml-configured (prefix package name, data source key)
 *    mapping; hot-updatable via [forceChangeDataSource].
 *  - [serviceDataSource]: runtime cache of resolved "specific service class
 *    package name -> data source key" results.
 *
 * Prefix matching is deterministic: a configured prefix matches a package only at package-segment
 * boundaries (`com.example.audit` matches `com.example.audit` and `com.example.audit.x`, but NOT
 * `com.example.auditor`), and when several prefixes match, the **longest** one wins.
 *
 * Concurrency: lock-free. [forceChangeDataSource] bumps a version stamp before clearing the
 * resolution cache; an in-flight resolution that started against the old mapping detects the bump
 * and skips caching its (possibly stale) result.
 *
 * @author hanson
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class MultipleDataSourceProperties : InitializingBean {
    private val log = LogFactory.getLog(this::class)

    /** yml-configured "package prefix -> data source key" mapping; can be hot-updated via [forceChangeDataSource]. */
    var packageDataSource = ConcurrentHashMap<String, String>()

    /**
     * AspectJ pointcut expression the routing advisor applies
     * [io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceInterceptor] with
     * (`kudos.ability.jdbc.routingPointcut`). Override it when business code does not live under
     * a `biz` sub-package.
     */
    var routingPointcut: String = "within(*..biz..*)"

    /**
     * Seconds a thread waits for another thread that is currently building the *same* dynamic data
     * source (`kudos.ability.jdbc.dataSourceCreateWaitSeconds`).
     *
     * Creation is serialized per data source id so a pool is never built twice. If the in-flight
     * creation hangs — unreachable host, DNS stall, a driver honouring no connect timeout — every
     * other request for that data source would queue behind it indefinitely, so the wait is
     * bounded and callers fail fast instead of piling up. Only the *first* request for a given
     * data source can ever wait; once registered, lookups take no lock.
     */
    var dataSourceCreateWaitSeconds: Long = 30

    private val serviceDataSource = ConcurrentHashMap<String, String>()

    /** bumped by [forceChangeDataSource] so concurrent resolutions against the old mapping are not cached */
    private val version = AtomicInteger()

    /**
     * Returns the data source key for a full package name (no prefix matching;
     * requires exact match against [packageDataSource]'s key). Null-safe: a null
     * key simply returns null.
     */
    fun lookUpKey(key: String?): String? = key?.let { packageDataSource[it] }

    /**
     * Looks up the data source key by the service class's package name: first checks the
     * [serviceDataSource] cache; if absent, resolves it by longest-prefix matching against
     * [packageDataSource] (at package-segment boundaries) and caches the result. Returns `""`
     * to indicate "no matching data source configuration" (also cached).
     */
    fun lookDataSourceKey(serviceClazz: Class<*>): String {
        val packageName = serviceClazz.packageName
        serviceDataSource[packageName]?.let { return it }
        val versionSnapshot = version.get()
        val resolved = packageDataSource.entries
            .filter { (prefix, _) -> packageName == prefix || packageName.startsWith("$prefix.") }
            .maxByOrNull { it.key.length }
            ?.value ?: ""
        if (versionSnapshot == version.get()) {
            serviceDataSource.putIfAbsent(packageName, resolved)
        }
        return resolved
    }

    /**
     * Force-changes the data source for a package path at runtime; also clears the
     * [serviceDataSource] cache so the next query re-resolves.
     */
    fun forceChangeDataSource(packageName: String, dataSource: String) {
        packageDataSource[packageName] = dataSource
        version.incrementAndGet()
        serviceDataSource.clear()
        log.info("Force-changed {0}, data source is: {1}", packageName, dataSource)
    }

    /** Spring [InitializingBean] callback: after bean assembly, prints the current data source configuration at DEBUG level. */
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        log.debug("Begin.....printing data source configuration....Begin")
        for (entry in packageDataSource.entries) {
            log.debug("{0}={1}", entry.key, entry.value)
        }
        log.debug("End.....printing data source configuration....End")
    }
}
