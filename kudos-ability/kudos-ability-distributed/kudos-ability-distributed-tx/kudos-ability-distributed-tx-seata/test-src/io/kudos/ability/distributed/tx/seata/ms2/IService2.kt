package io.kudos.ability.distributed.tx.seata.ms2

/**
 * 微服务应用2的service接口
 *
 * @author will
 * @since 5.1.1
 */
interface IService2 {
    fun increase(id: Int?, money: Double?)

    fun increaseFail(id: Int?, money: Double?)
}
