package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.model.contract.common.IHasBuiltIn

/**
 * 带 built_in 字段的测试表实体（用于 BaseCrudService 删除内置保护测试）
 */
internal interface TestBuiltInTableKtorm : IDbEntity<Int, TestBuiltInTableKtorm>, IHasBuiltIn {

    companion object Companion : DbEntityFactory<TestBuiltInTableKtorm>()

    /** 名称（用于条件删除/查询测试） */
    var name: String
}
