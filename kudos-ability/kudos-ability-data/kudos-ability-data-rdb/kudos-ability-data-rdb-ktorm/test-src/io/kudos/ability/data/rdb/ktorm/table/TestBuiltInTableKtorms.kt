package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.IntIdTable
import org.ktorm.schema.boolean
import org.ktorm.schema.varchar

/**
 * 带 built_in 字段的测试表
 */
internal object TestBuiltInTableKtorms : IntIdTable<TestBuiltInTableKtorm>("test_built_in_ktorm") {

    var name = varchar("name").bindTo { it.name }

    var builtIn = boolean("built_in").bindTo { it.builtIn }
}
