package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.IntIdTable
import org.ktorm.schema.*

/**
 * Table-to-entity binding object for the test table.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal object TestTableKtorms : IntIdTable<TestTableKtorm>("test_table_ktorm") {

    /** Name */
    var name = varchar("name").bindTo { it.name }

    /** Birthday */
    var birthday = datetime("birthday").bindTo { it.birthday }

    /** Whether active */
    var active = boolean("active").bindTo { it.active }

    /** Weight */
    var weight = double("weight").bindTo { it.weight }

    /** Height */
    var height = int("height").bindTo { it.height }




}