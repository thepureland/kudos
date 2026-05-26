package io.kudos.ability.data.rdb.ktorm.datasource

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.ktorm.database.Database
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
 * Returns the Database bound to the current thread; if none is bound, creates one from the default
 * DataSource and binds it to the current thread.
 *
 * @return Database bound to the current thread
 * @author K
 * @since 1.0.0
 */
fun KudosContextHolder.currentDatabase(): Database {
    var database = this.get().otherInfos?.get(KudosContext.OTHER_INFO_KEY_DATABASE)
    if (database == null) {
        database = Database.connectWithSpringSupport(currentDataSource(), alwaysQuoteIdentifiers = true)
        this.get().addOtherInfos(KudosContext.OTHER_INFO_KEY_DATABASE to database)
    }
    return database as Database
}