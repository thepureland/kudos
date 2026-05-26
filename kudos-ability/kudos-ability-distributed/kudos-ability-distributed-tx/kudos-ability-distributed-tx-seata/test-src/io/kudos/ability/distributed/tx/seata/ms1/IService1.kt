package io.kudos.ability.distributed.tx.seata.ms1

import io.kudos.ability.distributed.tx.seata.data.TestTable


/**
 * Service interface for microservice application 1.
 *
 * @author K
 * @since 1.0.0
 */
interface IService1 {

    fun getById(id: Int): TestTable

    fun decrease(id: Int, money: Double)

}
