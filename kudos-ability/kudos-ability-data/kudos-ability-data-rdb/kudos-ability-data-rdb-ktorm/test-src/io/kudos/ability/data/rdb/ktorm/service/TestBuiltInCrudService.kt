package io.kudos.ability.data.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.table.TestBuiltInTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestBuiltInTableKtormDao
import org.springframework.stereotype.Service

@Service
internal open class TestBuiltInCrudService : BaseCrudService<Int, TestBuiltInTableKtorm, TestBuiltInTableKtormDao>()
