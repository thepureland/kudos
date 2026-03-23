package io.kudos.ability.data.rdb.ktorm.service

import io.kudos.base.support.service.BaseCrudService
import io.kudos.ability.data.rdb.ktorm.table.TestBuiltInTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestBuiltInTableKtormDao
import org.springframework.stereotype.Service

@Service
internal open class TestBuiltInCrudService(
    dao: TestBuiltInTableKtormDao
) : BaseCrudService<Int, TestBuiltInTableKtorm, TestBuiltInTableKtormDao>(dao)
