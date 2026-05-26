package io.kudos.ability.data.rdb.ktorm.kit

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.context.core.KudosContextHolder
import org.ktorm.database.Database


/**
 * Returns the database object of the current context.
 *
 * @return Database object of the current context
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
fun RdbKit.getDatabase(): Database = KudosContextHolder.currentDatabase()