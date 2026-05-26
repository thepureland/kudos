package io.kudos.ability.distributed.tx.seata.data

import io.kudos.ability.data.rdb.ktorm.support.IntIdTable
import org.ktorm.schema.double

/**
 * Test-table data access object.
 *
 * @author K
 * @since 1.0.0
 */
object TestTables : IntIdTable<TestTable>("test_table") {

    var balance = double("balance").bindTo { it.balance }

}
