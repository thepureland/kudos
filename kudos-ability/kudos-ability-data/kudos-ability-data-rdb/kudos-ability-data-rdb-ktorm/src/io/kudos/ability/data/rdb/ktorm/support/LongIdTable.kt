package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.schema.Table
import org.ktorm.schema.long

/**
 * DAO for tables with a Long primary key.
 *
 * @param E Entity type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class LongIdTable<E : IDbEntity<Long, E>>(tableName: String): Table<E>(tableName) {

    /** Primary key */
    val id = long("id").primaryKey().bindTo { it.id }

}