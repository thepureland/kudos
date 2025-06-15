package io.kudos.ability.data.rdb.ktorm.datasource

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.ktorm.database.Database
import javax.sql.DataSource


/**
 * 返回当前线程关联的数据源，如果没有，取默认数据源，并将其与当前线程关联
 *
 * @return 当前线程关联的数据源
 * @author K
 * @since 1.0.0
 */
fun KudosContextHolder.currentDataSource(): DataSource {
    var dataSource = this.get().otherInfos?.get(KudosContext.OTHER_INFO_KEY_DATA_SOURCE)
    if (dataSource == null) {
        dataSource = SpringKit.getBean("dataSource")
//        setCurrentDataSource(dataSource as DataSource)
    }
    return dataSource as DataSource
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
//        setCurrentDatabase(database)
    }
    return database as Database
}