package io.kudos.ability.distributed.tx.seata.ms1

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * 微服务应用1的Controller
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/controller1")
open class Controller1 {

    @Autowired
    private lateinit var service1: IService1

    @RequestMapping("/getById")
    fun getById(id: Int) = service1.getById(id)

    /**
     * 扣减账户余额
     */
    @RequestMapping("/decrease")
    fun decrease(@RequestParam("id") id: Int, @RequestParam("money") money: Double) {
        println("########## decrease: $id    $money")
        service1.decrease(id, money)
    }

}
