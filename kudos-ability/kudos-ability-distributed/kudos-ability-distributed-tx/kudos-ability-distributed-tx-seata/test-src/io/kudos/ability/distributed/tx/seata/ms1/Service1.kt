package io.kudos.ability.distributed.tx.seata.ms1

import io.seata.core.context.RootContext
import org.soul.ability.distributed.tx.seata.data.TestTable
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.stereotype.Service

/**
 * 微服务应用1的service
 *
 * @author will
 * @since 5.1.1
 */
@Service
class Service1 : IService1 {
    @Autowired
    private val testTableMapper: TestTableMapper? = null

    private val log: Log = LogFactory.getLog(Service1::class.java)

    override fun getById(id: Int?): TestTable {
        return testTableMapper.get(id)
    }

    //    @Transactional // 可加可不加
    override fun decrease(id: Int?, money: Double?) {
        log.info("seata全局事务id【%s】".formatted(RootContext.getXID()))
        val entity: TestTable = testTableMapper.get(id)
        log.info("用户【%s】当前余额【%s】".formatted(id, entity.getBalance()))
        log.info("为用户【%s】扣减余额【%s】".formatted(id, money))
        entity.setBalance(entity.getBalance() - money)
        testTableMapper.updateById(entity)
        log.info("用户【%s】当前余额【%s】".formatted(id, testTableMapper.get(id).getBalance()))
    }
}
