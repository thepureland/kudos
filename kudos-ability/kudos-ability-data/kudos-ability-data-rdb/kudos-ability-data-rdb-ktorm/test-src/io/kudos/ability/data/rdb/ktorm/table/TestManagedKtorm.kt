package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

/**
 * Test entity with management fields (implements IAuditable via IManagedDbEntity),
 * used to verify the audit-column auto-fill paths in BaseCrudDao / AuditDefaults.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal interface TestManagedKtorm : IManagedDbEntity<String, TestManagedKtorm> {

    companion object Companion : DbEntityFactory<TestManagedKtorm>()

    /** Name */
    var name: String?
}
