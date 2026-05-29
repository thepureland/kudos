package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.schema.BaseTable
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache of Ktorm [BaseTable] instances keyed by shard / physical name, for use cases where the
 * **table the query targets must vary per call** (date-suffix archives, per-tenant horizontal
 * sharding, etc).
 *
 * **Why a factory, not a thread-local override?** Ktorm's `BaseTable.tableName` is `final`
 * (verified against ktorm-core 4.x), so the "thread-local rewrites `tableName` at query-build time"
 * trick that MyBatis-Plus supports (and that soul's `ISoulTableNameHandler` plugs into) is not
 * available in Ktorm without forking the library. The idiomatic Ktorm pattern instead is to
 * parameterize the Table subclass's constructor by the physical name and instantiate per shard.
 *
 * This factory just adds the missing piece: **cache the per-shard instances** so the column-binding
 * cost runs once per physical name rather than once per query. Without the cache, every query
 * allocates a new Table, re-runs all `bindTo { ... }` registrations, and Ktorm cannot reuse any of
 * its internal column lookups.
 *
 * Usage:
 * ```kotlin
 * class AuditLogTable(physicalName: String) : Table<AuditLog>(physicalName) {
 *     val id = varchar("id").bindTo { it.id }
 *     val tenantId = varchar("tenant_id").bindTo { it.tenantId }
 *     val ts = datetime("ts").bindTo { it.ts }
 * }
 *
 * object AuditLogTables : ShardedTableFactory<AuditLogTable>(::AuditLogTable)
 *
 * // Query against a specific shard:
 * val table = AuditLogTables.get("audit_log_2026q1")
 * database.from(table).select().forEach { row -> ... }
 * ```
 *
 * **Lifecycle**: instances are cached for the lifetime of the JVM. Typical sharding policies
 * produce a bounded number of shard keys (one per quarter, one per tenant), so unbounded growth is
 * not a concern. If a deployment generates an unbounded keyspace (e.g. one shard per minute) wrap
 * the factory with an LRU cache or similar — out of scope for this minimal abstraction.
 *
 * **Thread-safety**: [ConcurrentHashMap.computeIfAbsent] guarantees the [factory] is called at most
 * once per key even under concurrent first-touch, so two threads racing on the same shard key both
 * see the same Table instance.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class ShardedTableFactory<T : BaseTable<*>>(
    /** Constructor of the Table subclass, taking the physical table name and returning the Table. */
    private val factory: (physicalName: String) -> T,
) {

    private val cache = ConcurrentHashMap<String, T>()

    /**
     * Returns the Table instance whose `tableName` is [physicalName]. Lazily constructed on first
     * access and cached for subsequent calls. Concurrent first-touches resolve to the same
     * instance — see [ConcurrentHashMap.computeIfAbsent].
     */
    fun get(physicalName: String): T = cache.computeIfAbsent(physicalName) { factory(it) }

    /** Snapshot of physical names currently cached — primarily for diagnostics / tests. */
    fun knownNames(): Set<String> = cache.keys.toSet()

    /**
     * Drops every cached Table instance. Provided for tests that need a clean slate; production
     * code rarely needs to evict (per-tenant / per-period shards usually live as long as the JVM).
     */
    fun clear() {
        cache.clear()
    }
}
