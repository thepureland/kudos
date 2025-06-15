package io.kudos.ability.distributed.tx.seata.ms2

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 微服务应用2的Controller
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/controller2")
open class Controller2 {

    @Autowired
    private lateinit var service2: IService2

    /**
     * 扣减账户余额
     */
    @RequestMapping("/increase")
    fun decrease(@RequestParam("id") id: Int, @RequestParam("money") money: Double) {
        service2.increase(id, money)
    }

    @RequestMapping("/increaseFail")
    fun increaseFail(id: Int, money: Double) {
        service2.increaseFail(id, money)
    }

}
