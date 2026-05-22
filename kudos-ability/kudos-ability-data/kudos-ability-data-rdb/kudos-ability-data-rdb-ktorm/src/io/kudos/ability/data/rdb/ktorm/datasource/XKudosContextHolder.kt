package io.kudos.ability.data.rdb.ktorm.datasource

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.ktorm.database.Database
import javax.sql.DataSource


/**
 * 返回当前线程关联的数据源，如果没有，取默认数据源，并将其与当前线程关联。
 *
 * **不再在此处对 DataSource 手动套一层 Seata proxy**。原实现通过
 * `IDataSourceProxy.proxyDatasource(beanDs)` 二次包装；问题：
 * - Spring TransactionManager 直接使用 bean `dataSource` 实例
 * - Ktorm 通过本函数拿到的是再包一层的 wrapper —— 与 Spring TX 不是同一实例
 * - 结果：`@Transactional` 打开的连接 与 Ktorm SQL 执行的连接互不相通；
 *   Seata server 收不到 BranchRegister；Ktorm 写下的数据所在的孤儿连接被
 *   Hikari 还池时回滚（`is-auto-commit=false`），所以业务数据消失。
 *
 * 推荐由 `spring.datasource.dynamic.seata=true` 让 baomidou dynamic-datasource
 * 在 bean 层就装好 Seata 代理。这样 Spring TX 和 Ktorm 共享同一个 DataSource
 * 实例 / 同一个连接 / 同一次 commit 拦截。
 *
 * @return 当前线程关联的数据源
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
 * 返回当前线程关联的数据库，如果没有，则用默认数据源创建一个，并将其与当前线程关联
 *
 * @return 当前线程关联的数据库
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