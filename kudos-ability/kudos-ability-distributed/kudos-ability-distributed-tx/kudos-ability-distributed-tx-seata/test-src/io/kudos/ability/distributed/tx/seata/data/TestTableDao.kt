package io.kudos.ability.distributed.tx.seata.data

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import org.springframework.stereotype.Repository

@Repository
open class TestTableDao : BaseCrudDao<Int, TestTable, TestTables>()