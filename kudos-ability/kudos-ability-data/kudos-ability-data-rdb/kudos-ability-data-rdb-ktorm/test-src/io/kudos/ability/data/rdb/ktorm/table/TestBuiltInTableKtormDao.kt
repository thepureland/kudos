package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import org.springframework.stereotype.Repository

@Repository
internal open class TestBuiltInTableKtormDao : BaseCrudDao<Int, TestBuiltInTableKtorm, TestBuiltInTableKtorms>()
