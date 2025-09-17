package io.kudos.ability.distributed.tx.seata.data

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity


/**
 * 测试表数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface TestTable : IDbEntity<Int, TestTable> {

    companion object : DbEntityFactory<TestTable>()

    var balance: Double

}
