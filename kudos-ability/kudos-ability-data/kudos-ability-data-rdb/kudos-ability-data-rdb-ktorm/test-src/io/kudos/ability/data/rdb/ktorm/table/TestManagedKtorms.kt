package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import org.ktorm.schema.varchar

/**
 * Table-to-entity binding object for the managed test table (audit auto-fill tests).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal object TestManagedKtorms : ManagedTable<TestManagedKtorm>("test_managed_ktorm") {

    /** Name */
    var name = varchar("name").bindTo { it.name }
}
