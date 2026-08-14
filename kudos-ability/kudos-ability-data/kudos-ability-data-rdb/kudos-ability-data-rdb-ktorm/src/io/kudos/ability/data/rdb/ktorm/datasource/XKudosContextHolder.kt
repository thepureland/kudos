package io.kudos.ability.data.rdb.ktorm.datasource

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.ktorm.database.Database
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
 * **Weak keys**, so that a datasource replaced at runtime — `DsContextProcessor.refreshDatasource`
 * rebuilds them when an admin edits `sys_datasource` — takes its cached `Database` with it once
 * nothing else references it. The alternative was a listener subscribed to the same cache-clean
 * signal the jdbc module uses, which buys the same cleanup at the cost of a new module dependency
 * and a hook to keep in sync; weak keys need neither and also cover replacements that arrive by
 * some other route.
 *
 * The synchronisation this costs is a monitor around a map lookup on a path whose next act is a
 * database round trip, and the map holds one entry per physical datasource — single digits in every
 * deployment this framework targets.
 */
private val databaseCache: MutableMap<DataSource, Database> =
    Collections.synchronizedMap(WeakHashMap<DataSource, Database>())

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
 * **Invalidation is not needed and not offered as a hook.** A refresh replaces datasource
 * *instances*, so a refreshed datasource is a new key that gets its own `Database` — a stale one can
 * never be served. The old entry then has a weakly-held key and goes away with it. [clearKtormDatabaseCache]
 * exists for tests, not as something production has to remember to call.
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
    databaseCache[key]?.let { return it }

    // Built outside the map so the metadata round trip does not run while holding the lock. Two
    // threads racing here may both build one; the loser's copy is discarded and Database is a
    // stateless wrapper, so that costs one extra metadata read rather than correctness.
    val database = Database.connectWithSpringSupport(routingDataSource, alwaysQuoteIdentifiers = true)
    return databaseCache.putIfAbsent(key, database) ?: database
}

/**
 * The datasource [dataSource] currently routes to, which is [dataSource] itself when it does not
 * route. Falls back to the routing datasource if baomidou cannot resolve one, so a misconfigured
 * route surfaces from the query rather than from here.
 */
private fun routedDataSourceOf(dataSource: DataSource): DataSource =
    runCatching { (dataSource as? DynamicRoutingDataSource)?.determineDataSource() }.getOrNull() ?: dataSource

/**
 * Drops every cached [Database]. Call after replacing datasource instances at runtime; the next
 * query rebuilds what it needs.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun clearKtormDatabaseCache() {
    databaseCache.clear()
}
