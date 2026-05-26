package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.model.contract.common.IHasBuiltIn

/**
 * Test table entity with a built_in field (used by BaseCrudService built-in delete-protection tests).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal interface TestBuiltInTableKtorm : IDbEntity<Int, TestBuiltInTableKtorm>, IHasBuiltIn {

    companion object Companion : DbEntityFactory<TestBuiltInTableKtorm>()

    /** Name (used by conditional delete/query tests). */
    var name: String
}
