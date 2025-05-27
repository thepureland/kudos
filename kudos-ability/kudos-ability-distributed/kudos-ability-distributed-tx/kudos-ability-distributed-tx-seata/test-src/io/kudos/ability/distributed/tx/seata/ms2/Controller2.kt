package io.kudos.ability.distributed.tx.seata.ms2

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 微服务应用2的Controller
 *
 * @author will
 * @since 5.1.1
 */
@RestController
@RequestMapping("/controller2")
class Controller2 {
    @Autowired
    private val service2: IService2? = null

    /**
     * 扣减账户余额
     */
    @RequestMapping("/increase")
    fun decrease(@RequestParam("id") id: Int?, @RequestParam("money") money: Double?) {
        service2!!.increase(id, money)
    }

    @RequestMapping("/increaseFail")
    fun increaseFail(id: Int?, money: Double?) {
        service2!!.increaseFail(id, money)
    }
}
