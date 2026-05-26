package io.kudos.ability.distributed.tx.seata.main

import io.kudos.ability.distributed.tx.seata.TxException
import io.kudos.ability.distributed.tx.seata.data.TestTable
import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.ability.distributed.tx.seata.ms1.IService1
import io.kudos.ability.distributed.tx.seata.ms1.Service1
import io.kudos.ability.distributed.tx.seata.ms2.IService2
import io.kudos.ability.distributed.tx.seata.ms2.Service2
import jakarta.annotation.Resource
import org.apache.seata.core.context.RootContext
import org.apache.seata.spring.annotation.GlobalTransactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service

/**
 * Service of the main application.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Import(Service1::class, Service2::class, TestTableDao::class)
open class Service : IService {

    @Value($$"${seata.data-source-proxy-mode}")
    private val dataSourceProxyMode: String? = null

    @Autowired
    private lateinit var service1: IService1

    @Autowired
    private lateinit var service2: IService2

    @Resource
    private lateinit var feignClient11: IFeignClient11

    @Resource
    private lateinit var feignClient12: IFeignClient12

    @Resource
    private lateinit var feignClient21: IFeignClient21

    @Resource
    private lateinit var feignClient22: IFeignClient22

    @Resource
    private lateinit var testTableDao: TestTableDao

    override fun getById(id: Int): TestTable =
        requireNotNull(testTableDao.get(id)) { "TestTable not found: $id" }

    @GlobalTransactional
    override fun getGlobalTxId(): String? = RootContext.getXID()

    @GlobalTransactional
    override fun normalLocal() {
        service1.decrease(1, 50.0) // debit
        service2.increase(2, 50.0) // credit
    }

    @GlobalTransactional
    override fun onBranchErrorLocal() {
        service1.decrease(1, 50.0) // debit
        service2.increaseFail(2, 50.0) // simulate a credit failure
    }

    @GlobalTransactional
    override fun onGlobalErrorLocal() {
        service1.decrease(1, 50.0) // debit
        service2.increase(2, 50.0) // credit
        throw TxException("Simulated terminal global-transaction error; the transaction must roll back.")
    }

    @GlobalTransactional
    override fun normalRemote() {
        requireNotNull(client1) { "Feign client not configured: dataSourceProxyMode=$dataSourceProxyMode" }.decrease(1, 50.0) // debit
        requireNotNull(client2) { "Feign client not configured: dataSourceProxyMode=$dataSourceProxyMode" }.increase(2, 50.0) // credit
    }

    @GlobalTransactional
    override fun onBranchErrorRemote() {
        requireNotNull(client1) { "Feign client not configured: dataSourceProxyMode=$dataSourceProxyMode" }.decrease(1, 50.0) // debit
        requireNotNull(client2) { "Feign client not configured: dataSourceProxyMode=$dataSourceProxyMode" }.increaseFail(2, 50.0) // simulate a credit failure
    }

    @GlobalTransactional
    override fun onGlobalErrorRemote() {
        requireNotNull(client1) { "Feign client not configured: dataSourceProxyMode=$dataSourceProxyMode" }.decrease(1, 50.0) // debit
        requireNotNull(client2) { "Feign client not configured: dataSourceProxyMode=$dataSourceProxyMode" }.increase(2, 50.0) // credit
        throw TxException("Simulated terminal global-transaction error; the transaction must roll back.")
    }

    private val client1: IClient1?
        get() {
            return when (dataSourceProxyMode) {
                "AT" -> feignClient12
                "XA" -> feignClient11
                else -> null
            }
        }

    private val client2: IClient2?
        get() {
            return when (dataSourceProxyMode) {
                "AT" -> feignClient22
                "XA" -> feignClient21
                else -> null
            }
        }

}
