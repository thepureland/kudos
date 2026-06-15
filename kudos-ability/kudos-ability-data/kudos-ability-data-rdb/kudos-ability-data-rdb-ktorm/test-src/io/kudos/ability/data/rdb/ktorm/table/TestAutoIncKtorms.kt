package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.IntIdTable
import org.ktorm.schema.varchar

/**
 * Table-to-entity binding object for the auto-increment test table.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal object TestAutoIncKtorms : IntIdTable<TestAutoIncKtorm>("test_auto_inc_ktorm") {

    /** Name */
    var name = varchar("name").bindTo { it.name }
}
