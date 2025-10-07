package io.kudos.ability.distributed.tx.seata.ms1

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ability.data.rdb.ktorm.datasource.currentDataSource
import io.kudos.ability.data.rdb.ktorm.kit.getDatabase
import io.kudos.ability.distributed.tx.seata.data.TestTable
import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.seata.core.context.RootContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource
import kotlin.test.assertFalse

/**
 * 微服务应用1的service
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class Service1 : IService1 {

    @Autowired
    private lateinit var testTableDao: TestTableDao

    private val log = LogFactory.getLog(this)

    override fun getById(id: Int): TestTable = testTableDao.get(id)!!

            @Transactional // 可加可不加
    override fun decrease(id: Int, money: Double) {
        val ds = KudosContextHolder.currentDataSource()
        ds.connection.use { conn ->
            println("conn.class = ${conn.javaClass.name}")                   // 你现在看到的是 HikariProxyConnection
            // 试图从外层unwrap到 Seata 的连接代理 —— 成功则一定已生效
//            val seataConn = try {
//                conn.unwrap(io.seata.rm.datasource.ConnectionProxy::class.java)
//            } catch (e: Throwable) {
//                null
//            }
//            println("seataConn? = ${seataConn != null}")
        }
        log.info("seata全局事务id【${RootContext.getXID()}】")
        val entity = testTableDao.get(id)!!
        log.info("用户【$id】当前余额【${entity.balance}】")
        log.info("为用户【$id】扣减余额【${money}】")
        entity.balance = entity.balance.minus(money)
        testTableDao.update(entity)
        log.info("用户【$id】当前余额【${testTableDao.get(id)!!.balance}】")
    }

}
