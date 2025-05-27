package io.kudos.ability.distributed.tx.seata.ms1

import org.soul.ability.distributed.tx.seata.data.TestTable

/**
 * 微服务应用1的Controller
 *
 * @author will
 * @since 5.1.1
 */
@RestController
@RequestMapping("/controller1")
class Controller1 {
    @Autowired
    private val service1: IService1? = null

    @RequestMapping("/getById")
    fun getById(id: Int?): TestTable? {
        return service1!!.getById(id)
    }

    /**
     * 扣减账户余额
     */
    @RequestMapping("/decrease")
    fun decrease(@RequestParam("id") id: Int?, @RequestParam("money") money: Double?) {
        println("########## decrease: " + id + "    " + money)
        service1!!.decrease(id, money)
    }
}
