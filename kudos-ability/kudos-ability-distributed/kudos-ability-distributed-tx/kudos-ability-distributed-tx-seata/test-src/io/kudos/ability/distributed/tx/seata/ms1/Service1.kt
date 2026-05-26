package io.kudos.ability.distributed.tx.seata.ms1

import io.kudos.ability.data.rdb.ktorm.datasource.currentDataSource
import io.kudos.ability.distributed.tx.seata.data.TestTable
import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import org.apache.seata.core.context.RootContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for microservice application 1.
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class Service1 : IService1 {

    @Autowired
    private lateinit var testTableDao: TestTableDao

    private val log = LogFactory.getLog(this::class)

    override fun getById(id: Int): TestTable =
        requireNotNull(testTableDao.get(id)) { "TestTable not found: $id" }

    @Transactional // Optional
    override fun decrease(id: Int, money: Double) {
        val ds = KudosContextHolder.currentDataSource()
        ds.connection.use { conn ->
            println("conn.class = ${conn.javaClass.name}")
        }
        log.info("Seata global transaction id [${RootContext.getXID()}]")
        val entity = requireNotNull(testTableDao.get(id)) { "TestTable not found: $id" }
        log.info("User [$id] current balance [${entity.balance}]")
        log.info("Deducting balance [${money}] for user [$id]")
        entity.balance = entity.balance.minus(money)
        testTableDao.update(entity)
        val after = requireNotNull(testTableDao.get(id)) { "TestTable not found: $id" }
        log.info("User [$id] current balance [${after.balance}]")
    }

}
