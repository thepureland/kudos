package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.IntIdTable
import org.ktorm.schema.boolean
import org.ktorm.schema.varchar

/**
 * Test table that includes a built_in field.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal object TestBuiltInTableKtorms : IntIdTable<TestBuiltInTableKtorm>("test_built_in_ktorm") {

    var name = varchar("name").bindTo { it.name }

    var builtIn = boolean("built_in").bindTo { it.builtIn }
}
