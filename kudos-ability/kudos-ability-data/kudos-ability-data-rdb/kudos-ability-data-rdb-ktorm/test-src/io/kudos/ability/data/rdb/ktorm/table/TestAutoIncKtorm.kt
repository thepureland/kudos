package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity

/**
 * Test entity for the auto-increment-primary-key table, used to verify the
 * "insert without an explicit id" path of BaseCrudDao.insert.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal interface TestAutoIncKtorm : IDbEntity<Int, TestAutoIncKtorm> {

    companion object Companion : DbEntityFactory<TestAutoIncKtorm>()

    /** Name */
    var name: String?
}
