package io.kudos.ability.data.rdb.jdbc.init

import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.InitializingBean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.Volatile

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
 * The first time a specific class's package path is queried, it is resolved once
 * using [packageDataSource]'s prefix-match rules and cached; a hot update clears
 * the whole cache so the next query resolves again.
 *
 * Known issue: [lookDataSourceKey] uses both `ConcurrentHashMap.computeIfAbsent`
 * and the readLock; the former is already thread-safe, the latter only "pairs"
 * with [forceChangeDataSource]'s writeLock and does not actually guard anything
 * (see the README limitations list).
 *
 * @author hanson
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class MultipleDataSourceProperties : InitializingBean {
    private val log = LogFactory.getLog(this::class)

    /** yml-configured "package prefix -> data source key" mapping; can be hot-updated via [forceChangeDataSource]. */
    @Volatile
    var packageDataSource = ConcurrentHashMap<String, String>()
    private val serviceDataSource = ConcurrentHashMap<String, String>()
    private val rw: ReadWriteLock = ReentrantReadWriteLock()

    /**
     * Returns the data source key for a full package name (no prefix matching;
     * requires exact match against [packageDataSource]'s key).
     */
    fun lookUpKey(key: String?): String? {
        if (packageDataSource.containsKey(key)) {
            return packageDataSource.get(key)
        }
        return null
    }

    /**
     * Looks up the data source key by the service class's package name: first
     * checks the [serviceDataSource] cache; if absent, resolves it using
     * [packageDataSource]'s prefix matching and caches the result. Returns `""`
     * to indicate "no matching data source configuration".
     */
    fun lookDataSourceKey(serviceClazz: Class<*>): String {
        val packageName = serviceClazz.getPackageName()
        rw.readLock().lock()
        try {
            var result = serviceDataSource.get(packageName)
            if (result.isNullOrBlank()) {
                result = serviceDataSource.computeIfAbsent(packageName) { _ ->
                    for (entry in packageDataSource.entries) {
                        if (packageName.startsWith(entry.key)) {
                            return@computeIfAbsent entry.value
                        }
                    }
                    ""
                }
            }
            return result
        } finally {
            rw.readLock().unlock()
        }
    }

    /**
     * Force-changes the data source for a package path at runtime; also clears the
     * [serviceDataSource] cache so the next query re-resolves. Thread-safe
     * (protected by writeLock).
     */
    fun forceChangeDataSource(packageName: String, dataSource: String) {
        packageDataSource[packageName] = dataSource
        rw.writeLock().lock()
        try {
            serviceDataSource.clear()
        } finally {
            rw.writeLock().unlock()
        }
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
