package io.kudos.ability.distributed.tx.seata.ms1

import io.kudos.ability.distributed.tx.seata.data.TestTable
import io.kudos.ability.distributed.tx.seata.data.TestTableMapper
import io.kudos.base.logger.LogFactory
import io.seata.core.context.RootContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 微服务应用1的service
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class Service1 : IService1 {

    @Autowired
    private lateinit var testTableMapper: TestTableMapper

    private val log = LogFactory.getLog(this)

    override fun getById(id: Int): TestTable = testTableMapper.get(id)

    //    @Transactional // 可加可不加
    override fun decrease(id: Int, money: Double) {
        log.info("seata全局事务id【${RootContext.getXID()}】")
        val entity: TestTable = testTableMapper.get(id)
        log.info("用户【$id】当前余额【${entity.balance}】")
        log.info("为用户【$id】扣减余额【${money}】")
        entity.balance = entity.balance?.minus(money)
        testTableMapper.updateById(entity)
        log.info("用户【$id】当前余额【${testTableMapper.get(id).balance}】")
    }

}
