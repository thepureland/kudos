package io.kudos.ability.distributed.tx.seata.main

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

interface IClient2 {

    @RequestMapping("/controller2/increase")
    fun increase(@RequestParam("id") id: Int, @RequestParam("money") money: Double)

    @RequestMapping("/controller2/increaseFail")
    fun increaseFail(@RequestParam("id") id: Int, @RequestParam("money") money: Double)

}
