package io.kudos.ability.distributed.tx.seata.main

import io.kudos.ability.distributed.tx.seata.data.TestTable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam


interface IClient1 {

    @RequestMapping("/controller1/getById")
    fun getById(@RequestParam("id") id: Int): TestTable

    /**
     * 扣减账户余额
     */
    @GetMapping("/controller1/decrease")
    fun decrease(@RequestParam("id") id: Int, @RequestParam("money") money: Double)

}
