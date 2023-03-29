package io.kudos.ability.data.rdb.ktorm.kit

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.context.core.KudosContextHolder
import org.ktorm.database.Database


/**
 * 取得当前上下文的数据库对象
 *
 * @return 当前上下文的数据库对象
 * @author K
 * @since 1.0.0
 */
fun RdbKit.getDatabase(): Database = KudosContextHolder.currentDatabase()