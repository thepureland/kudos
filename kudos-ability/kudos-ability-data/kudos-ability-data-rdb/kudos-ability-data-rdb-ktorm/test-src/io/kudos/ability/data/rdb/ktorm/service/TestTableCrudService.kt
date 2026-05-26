package io.kudos.ability.data.rdb.ktorm.service

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtormDao
import org.springframework.stereotype.Service

/** Control Service that does not implement IHasBuiltIn. */
@Service
internal open class TestTableCrudService(
    dao: TestTableKtormDao
) : BaseCrudService<Int, TestTableKtorm, TestTableKtormDao>(dao)
