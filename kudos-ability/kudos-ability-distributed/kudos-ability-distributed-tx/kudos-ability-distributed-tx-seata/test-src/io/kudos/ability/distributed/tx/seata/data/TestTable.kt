package io.kudos.ability.distributed.tx.seata.data

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity


/**
 * Test-table database entity.
 *
 * @author K
 * @since 1.0.0
 */
interface TestTable : IDbEntity<Int, TestTable> {

    companion object : DbEntityFactory<TestTable>()

    var balance: Double

}
