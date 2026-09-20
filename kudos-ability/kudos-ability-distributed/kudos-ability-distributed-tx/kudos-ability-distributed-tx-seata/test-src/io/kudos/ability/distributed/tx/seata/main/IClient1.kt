package io.kudos.ability.distributed.tx.seata.main

import io.kudos.ability.distributed.tx.seata.data.TestTable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange


interface IClient1 {

    @GetExchange("/controller1/getById")
    fun getById(@RequestParam("id") id: Int): TestTable

    /**
     * Decrease the account balance.
     */
    @GetExchange("/controller1/decrease")
    fun decrease(@RequestParam("id") id: Int, @RequestParam("money") money: Double)

}
