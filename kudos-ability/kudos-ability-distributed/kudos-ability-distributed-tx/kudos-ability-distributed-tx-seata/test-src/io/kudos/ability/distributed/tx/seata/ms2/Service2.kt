package io.kudos.ability.distributed.tx.seata.ms2

import io.seata.core.context.RootContext
import org.soul.ability.distributed.tx.seata.TxException
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.stereotype.Service

/**
 * 微服务应用2的service
 *
 * @author will
 * @since 5.1.1
 */
@Service
class Service2 : IService2 {
    @Autowired
    private val testTableMapper: TestTableMapper? = null

    private val log: Log = LogFactory.getLog(Service2::class.java)

    //    @Transactional // 可加可不加
    override fun increase(id: Int?, money: Double?) {
        log.info("seata全局事务id【%s】".formatted(RootContext.getXID()))
        val entity: TestTable = testTableMapper.get(id)
        log.info("用户【%s】当前余额【%s】".formatted(id, entity.getBalance()))
        log.info("为用户【%s】增加余额【%s】".formatted(id, money))
        entity.setBalance(entity.getBalance() + money)
        testTableMapper.updateById(entity)
        log.info("用户【%s】当前余额【%s】".formatted(id, testTableMapper.get(id).getBalance()))
    }

    //    @Transactional // 可加可不加
    override fun increaseFail(id: Int?, money: Double?) {
        log.info("seata全局事务id【%s】".formatted(RootContext.getXID()))
        throw TxException("模拟加款时错误发生，事务回滚.")
    }
}
