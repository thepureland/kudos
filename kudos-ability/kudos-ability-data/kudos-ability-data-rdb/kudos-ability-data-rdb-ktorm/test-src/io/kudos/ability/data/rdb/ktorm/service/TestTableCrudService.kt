package io.kudos.ability.data.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtormDao
import org.springframework.stereotype.Service

/** 无 IHasBuiltIn 的对照用 Service */
@Service
internal open class TestTableCrudService : BaseCrudService<Int, TestTableKtorm, TestTableKtormDao>()
