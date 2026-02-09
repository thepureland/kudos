package io.kudos.ability.distributed.tx.seata.ms2

import io.kudos.ability.distributed.tx.seata.TxException
import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.base.logger.LogFactory
import org.apache.seata.core.context.RootContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 微服务应用2的service
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class Service2 : IService2 {

    @Autowired
    private lateinit var testTableDao: TestTableDao

    private val log = LogFactory.getLog(this)

        @Transactional // 可加可不加
    override fun increase(id: Int, money: Double) {
        log.info("seata全局事务id【${RootContext.getXID()}】")
        val entity = testTableDao.get(id)!!
        log.info("用户【$id】当前余额【${entity.balance}】")
        log.info("为用户【$id】增加余额【${money}】")
        entity.balance = entity.balance.plus(money)
        testTableDao.update(entity)
        log.info("用户【$id】当前余额【${testTableDao.get(id)!!.balance}】")
    }

    //    @Transactional // 可加可不加
    override fun increaseFail(id: Int, money: Double) {
        log.info("seata全局事务id【${RootContext.getXID()}】")
        throw TxException("模拟加款时错误发生，事务回滚.")
    }

}
