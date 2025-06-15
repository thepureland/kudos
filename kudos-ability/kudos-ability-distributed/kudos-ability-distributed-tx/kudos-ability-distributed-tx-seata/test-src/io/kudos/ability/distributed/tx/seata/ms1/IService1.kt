package io.kudos.ability.distributed.tx.seata.ms1

import io.kudos.ability.distributed.tx.seata.data.TestTable


/**
 * 微服务应用1的service接口
 *
 * @author K
 * @since 1.0.0
 */
interface IService1 {

    fun getById(id: Int): TestTable

    fun decrease(id: Int, money: Double)

}
