package io.kudos.ability.distributed.tx.seata.ms2

/**
 * Service interface for microservice application 2.
 *
 * @author K
 * @since 1.0.0
 */
interface IService2 {

    fun increase(id: Int, money: Double)

    fun increaseFail(id: Int, money: Double)

}
