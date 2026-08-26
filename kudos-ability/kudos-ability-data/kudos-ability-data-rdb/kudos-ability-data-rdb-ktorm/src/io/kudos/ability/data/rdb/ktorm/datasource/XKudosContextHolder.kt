package io.kudos.ability.data.rdb.ktorm.datasource

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.ktorm.database.Database
import org.ktorm.logging.detectLoggerImplementation
import io.kudos.ability.data.rdb.ktorm.support.RedactingKtormLogger
import java.util.Collections
import java.util.WeakHashMap
import javax.sql.DataSource


/**
 * Returns the DataSource bound to the current thread; if none is bound, fetches the default
 * DataSource and binds it to the current thread.
 *
 * **No manual Seata proxy wrapping of the DataSource happens here anymore.** The previous
 * implementation wrapped via `IDataSourceProxy.proxyDatasource(beanDs)`; the problem was:
 * - Spring's TransactionManager uses the bean `dataSource` instance directly.
 * - Ktorm obtained a doubly-wrapped wrapper from this function — not the same instance Spring TX saw.
 * - Result: connections opened by `@Transactional` and connections used by Ktorm SQL were disjoint;
 *   the Seata server never received BranchRegister; the orphan connection holding Ktorm-written
 *   data was rolled back by Hikari on return-to-pool (`is-auto-commit=false`), so business data
 *   disappeared.
 *
 * Recommendation: set `spring.datasource.dynamic.seata=true` so baomidou dynamic-datasource installs
 * the Seata proxy at the bean layer. Spring TX and Ktorm then share the same DataSource instance,
 * the same connection, and the same commit interception.
 *
 * The bean is looked up by the name `dataSource` rather than by type, matching
 * [io.kudos.ability.data.rdb.jdbc.kit.RdbKit.getDataSource]. By-type lookup would be ambiguous in
 * any deployment that registers more than one `DataSource` bean.
 *
 * @return DataSource bound to the current thread
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
fun KudosContextHolder.currentDataSource(): DataSource {
    val cached = this.get().otherInfos?.get(KudosContext.OTHER_INFO_KEY_DATA_SOURCE)
    if (cached != null) return cached as DataSource
    val dataSource = SpringKit.getBean("dataSource") as DataSource
    this.get().addOtherInfos(KudosContext.OTHER_INFO_KEY_DATA_SOURCE to dataSource)
    return dataSource
}

/**
 * Ktorm [Database] objects, one per physical datasource. See [currentDatabase].
 *
 * Not request-scoped, because constructing a [Database] is far from free: ktorm's `init` block
 * borrows a pooled connection and issues around fifteen `DatabaseMetaData` calls to learn the
 * product name, the reserved-word list, the identifier quoting rules and the identifier case rules.
 * Paying that on every request means a connection checkout and a round trip per request for values
 * that cannot change.
 *
 * **Weak keys** let a datasource replaced at runtime — `DsContextProcessor.refreshDatasource` rebuilds them
 * when an admin edits `sys_datasource` — take its cached `Database` with it once nothing else references it.
 *
 * They are not sufficient on their own, and it took a recurring test failure to show why. A `WeakHashMap`
 * holds keys weakly but **values strongly**, and the value here captures the routing datasource passed to
 * `connectWithSpringSupport` while the key is whatever that routing resolved to. With nothing routing they are
 * the same object, so the value strongly references its own key and the entry is immortal; with routing, the
 * key can be a long-lived inner datasource while the value still points at one context's routing datasource.
 * Either way an entry can outlive the pool behind it, and the failure lands far from here — a
 * `ProxyConnection.close()` with a null delegate, inside an unrelated query.
 *
 * **Each entry therefore records the routing datasource it was built over**, which is what makes an entry's
 * owner answerable. [KtormDatabaseCacheEvictor] retires the datasources of a closing context, and an entry
 * matching on either side — the key it was filed under or the routing datasource it wraps — goes with them.
 * An earlier version cleared the whole cache instead, because the key alone could not say which entries
 * belonged to the closing context; that also threw away entries belonging to contexts still running, which is
 * wrong as soon as more than one context shares a JVM.
 *
 * [clearKtormDatabaseCache] remains available for tests and for code that replaces datasources by some other
 * route.
 *
 * The synchronisation this costs is a monitor around a map lookup on a path whose next act is a
 * database round trip, and the map holds one entry per physical datasource — single digits in every
 * deployment this framework targets.
 */
private val databaseCache: MutableMap<DataSource, CachedDatabase> =
    Collections.synchronizedMap(WeakHashMap<DataSource, CachedDatabase>())

/** A cached [Database] together with the routing datasource it takes connections from. */
private class CachedDatabase(val database: Database, val routingDataSource: DataSource)

/**
 * Datasources whose pools are going away, so nothing may be cached against them again.
 *
 * Clearing the cache on close is not enough on its own: `ContextClosedEvent` is published *before* the
 * context destroys its singletons, so a query already in flight can rebuild an entry over a pool that is
 * about to shut down, and that entry then outlives the close it was supposed to be removed by. Marking the
 * datasource instead makes the removal stick — the in-flight query still gets a `Database` and still races
 * the shutdown, but it can no longer leave one behind for the callers that come after.
 *
 * Weak, so a retired datasource stops being tracked once nothing else holds it, and identity-based, because
 * two pools are the same pool only if they are the same object. A datasource rebuilt by
 * `DsContextProcessor.refreshDatasource` is a new instance and therefore not retired.
 */
private val retiredDataSources: MutableSet<DataSource> =
    Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<DataSource, Boolean>()))

/**
 * Returns the Ktorm [Database] for the datasource the current thread routes to.
 *
 * Resolution order:
 * 1. An explicit `Database` placed in [KudosContext.otherInfos] wins — that is how the code
 *    generator and tests pin a specific database.
 * 2. Otherwise the cached `Database` for the datasource this thread currently routes to.
 *
 * **Why the cache is keyed by the *routed-to* datasource rather than being a single global.** The
 * metadata ktorm reads at construction (reserved words, quote character, identifier case folding)
 * describes the database the connection actually reached. A deployment that routes one
 * `DynamicRoutingDataSource` to different products — this framework ships exactly that, with the
 * ClickHouse audit log alongside a PostgreSQL main database — would get one product's quoting rules
 * applied to the other's SQL if a single `Database` were shared. Keying by the resolved datasource
 * keeps one `Database` per physical database, which is both correct and the point of the cache.
 *
 * The `Database` still wraps the **routing** DataSource, never the resolved one: Spring's
 * `TransactionManager` is bound to the routing bean, so connections must be taken from it to join
 * the ambient transaction. Handing ktorm the resolved datasource instead is the historical bug
 * described on [currentDataSource].
 *
 * **Invalidation on refresh is not needed.** A refresh replaces datasource *instances*, so a refreshed
 * datasource is a new key that gets its own `Database` — a stale one can never be served on that path.
 *
 * **Invalidation on context close is needed**, and used to be missing. The old entry is not reliably collected
 * with its weak key, because the value can reference the key or outlive the pool the key resolved to; a
 * `Database` bound to a closed context's routing datasource then hands out connections from a pool that has
 * been shut down. [KtormDatabaseCacheEvictor] clears the cache on `ContextClosedEvent` for exactly that
 * reason. [clearKtormDatabaseCache] stays available for tests and for datasource replacements that arrive by
 * some other route.
 *
 * @return the Database for the current thread's datasource
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun KudosContextHolder.currentDatabase(): Database {
    val pinned = this.get().otherInfos?.get(KudosContext.OTHER_INFO_KEY_DATABASE)
    if (pinned != null) return pinned as Database

    val routingDataSource = currentDataSource()
    val key = routedDataSourceOf(routingDataSource)
    val retired = isRetired(key) || isRetired(routingDataSource)
    if (!retired) databaseCache[key]?.let { return it.database }

    // Built outside the map so the metadata round trip does not run while holding the lock. Two
    // threads racing here may both build one; the loser's copy is discarded and Database is a
    // stateless wrapper, so that costs one extra metadata read rather than correctness.
    val database = Database.connectWithSpringSupport(
        routingDataSource,
        logger = RedactingKtormLogger(detectLoggerImplementation()),
        alwaysQuoteIdentifiers = true,
    )
    // Retired between the check above and here, or already retired on entry: hand this caller its Database
    // and leave nothing behind, so the shutdown it is racing does not become the next caller's problem.
    if (retired || isRetired(key) || isRetired(routingDataSource)) return database
    return (databaseCache.putIfAbsent(key, CachedDatabase(database, routingDataSource)))?.database ?: database
}

private fun isRetired(dataSource: DataSource): Boolean = retiredDataSources.contains(dataSource)

/**
 * Drops the cached [Database] objects belonging to [dataSources] and refuses to cache against them again.
 *
 * Called when a context closes, with the datasources that context owns. Entries are matched on both the key
 * they were filed under and the routing datasource they wrap, because routing means those are not the same
 * object: the key is what the routing resolved to, and either side going away makes the entry unusable.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun retireKtormDatabases(dataSources: Collection<DataSource>) {
    if (dataSources.isEmpty()) return
    val retiring = Collections.newSetFromMap(java.util.IdentityHashMap<DataSource, Boolean>())
    retiring.addAll(dataSources)
    retiredDataSources.addAll(retiring)
    synchronized(databaseCache) {
        databaseCache.entries.removeIf { (key, cached) ->
            key in retiring || cached.routingDataSource in retiring
        }
    }
}

/**
 * The datasource [dataSource] currently routes to, which is [dataSource] itself when it does not
 * route. Falls back to the routing datasource if baomidou cannot resolve one, so a misconfigured
 * route surfaces from the query rather than from here.
 */
private fun routedDataSourceOf(dataSource: DataSource): DataSource =
    runCatching { (dataSource as? DynamicRoutingDataSource)?.determineDataSource() }.getOrNull() ?: dataSource

/**
 * Drops every cached [Database] and un-retires every datasource. Call after replacing datasource instances at
 * runtime; the next query rebuilds what it needs.
 *
 * A full reset, unlike [retireKtormDatabases]: it makes no claim about which pools are going away, so it must
 * not leave retirement marks that would stop a still-live datasource from ever being cached again.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun clearKtormDatabaseCache() {
    databaseCache.clear()
    retiredDataSources.clear()
}
