package io.kudos.ability.distributed.tx.seata.ms2

import io.kudos.ability.distributed.tx.seata.TxException
import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.base.logger.LogFactory
import org.apache.seata.core.context.RootContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service of microservice application 2.
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class Service2 : IService2 {

    @Autowired
    private lateinit var testTableDao: TestTableDao

    private val log = LogFactory.getLog(this::class)

    @Transactional // optional
    override fun increase(id: Int, money: Double) {
        log.info("Seata global transaction id [${RootContext.getXID()}]")
        val entity = requireNotNull(testTableDao.get(id)) { "TestTable not found: $id" }
        log.info("User [$id] current balance [${entity.balance}]")
        log.info("Credit user [$id] balance by [${money}]")
        entity.balance = entity.balance.plus(money)
        testTableDao.update(entity)
        val after = requireNotNull(testTableDao.get(id)) { "TestTable not found: $id" }
        log.info("User [$id] current balance [${after.balance}]")
    }

    //    @Transactional // optional
    override fun increaseFail(id: Int, money: Double) {
        log.info("Seata global transaction id [${RootContext.getXID()}]")
        throw TxException("Simulated error while crediting; the transaction is rolled back.")
    }

}
