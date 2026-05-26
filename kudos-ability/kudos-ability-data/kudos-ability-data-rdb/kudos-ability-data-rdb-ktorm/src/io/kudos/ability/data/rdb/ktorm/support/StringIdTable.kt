package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.schema.Table
import org.ktorm.schema.varchar

/**
 * DAO for tables with a String primary key.
 *
 * @param E Entity type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class StringIdTable<E : IDbEntity<String, E>>(tableName: String): Table<E>(tableName) {

    /** Primary key */
    val id = varchar("id").primaryKey().bindTo { it.id }

}