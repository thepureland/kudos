package io.kudos.ability.distributed.tx.seata.ms1

import org.soul.ability.distributed.tx.seata.data.TestTable

/**
 * 微服务应用1的service接口
 *
 * @author will
 * @since 5.1.1
 */
interface IService1 {
    fun getById(id: Int?): TestTable?

    fun decrease(id: Int?, money: Double?)
}
