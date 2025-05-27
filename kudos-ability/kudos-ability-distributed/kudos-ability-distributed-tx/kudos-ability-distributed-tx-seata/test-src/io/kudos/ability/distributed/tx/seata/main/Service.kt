package io.kudos.ability.distributed.tx.seata.main

import io.seata.core.context.RootContext
import io.seata.spring.annotation.GlobalTransactional
import org.soul.ability.distributed.tx.seata.TxException
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 主应用的service
 *
 * @author will
 * @since 5.1.1
 */
@Service
@ComponentScan(
    basePackages = ["org.soul.ability.distributed.tx.seata.ms1", "org.soul.ability.distributed.tx.seata.ms2"
    ]
)
class Service : IService {
    @Value("\${seata.data-source-proxy-mode}")
    private val dataSourceProxyMode: String? = null

    @Autowired
    private val service1: IService1? = null

    @Autowired
    private val service2: IService2? = null

    @Autowired
    private val feignClient11: IFeignClient11? = null

    @Autowired
    private val feignClient12: IFeignClient12? = null

    @Autowired
    private val feignClient21: IFeignClient21? = null

    @Autowired
    private val feignClient22: IFeignClient22? = null

    @Autowired
    private val testTableMapper: TestTableMapper? = null

    private val log: Log? = LogFactory.getLog(Service::class.java)

    override fun getById(id: Int?): TestTable {
        return testTableMapper.get(id)
    }

    @GlobalTransactional
    override fun getGlobalTxId(): String? {
        return RootContext.getXID()
    }

    @GlobalTransactional
    override fun normalLocal() {
        service1.decrease(1, 50.0) // 扣款
        service2.increase(2, 50.0) // 加款
    }

    @GlobalTransactional
    override fun onBranchErrorLocal() {
        service1.decrease(1, 50.0) // 扣款
        service2.increaseFail(2, 50.0) // 模拟加款失败
    }

    @GlobalTransactional
    override fun onGlobalErrorLocal() {
        service1.decrease(1, 50.0) // 扣款
        service2.increase(2, 50.0) // 加款
        throw TxException("模拟全局事务最后错误发生，事务应回滚.")
    }

    @GlobalTransactional
    override fun normalRemote() {
        this.client1!!.decrease(1, 50.0) // 扣款
        this.client2!!.increase(2, 50.0) // 加款
    }

    @GlobalTransactional
    override fun onBranchErrorRemote() {
        this.client1!!.decrease(1, 50.0) // 扣款
        this.client2!!.increaseFail(2, 50.0) // 模拟加款失败
    }

    @GlobalTransactional
    override fun onGlobalErrorRemote() {
        this.client1!!.decrease(1, 50.0) // 扣款
        this.client2!!.increase(2, 50.0) // 加款
        throw TxException("模拟全局事务最后错误发生，事务应回滚.")
    }

    private val client1: IClient1?
        get() {
            if ("AT" == dataSourceProxyMode) {
                return feignClient12
            } else if ("XA" == dataSourceProxyMode) {
                return feignClient11
            } else {
                return null
            }
        }

    private val client2: IClient2?
        get() {
            if ("AT" == dataSourceProxyMode) {
                return feignClient22
            } else if ("XA" == dataSourceProxyMode) {
                return feignClient21
            } else {
                return null
            }
        }
}
