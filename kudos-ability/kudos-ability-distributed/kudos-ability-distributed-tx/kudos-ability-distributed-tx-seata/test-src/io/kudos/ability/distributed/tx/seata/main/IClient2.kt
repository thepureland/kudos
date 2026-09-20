package io.kudos.ability.distributed.tx.seata.main

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface IClient2 {

    @GetExchange("/controller2/increase")
    fun increase(@RequestParam("id") id: Int, @RequestParam("money") money: Double)

    @GetExchange("/controller2/increaseFail")
    fun increaseFail(@RequestParam("id") id: Int, @RequestParam("money") money: Double)

}
