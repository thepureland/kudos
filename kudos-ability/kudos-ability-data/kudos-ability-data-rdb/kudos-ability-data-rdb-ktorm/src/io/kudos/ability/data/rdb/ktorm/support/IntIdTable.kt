package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.schema.Table
import org.ktorm.schema.int

/**
 * DAO for tables with an Int primary key.
 *
 * @param E Entity type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class IntIdTable<E : IDbEntity<Int, E>>(tableName: String): Table<E>(tableName) {

    /** Primary key */
    val id = int("id").primaryKey().bindTo { it.id }

}