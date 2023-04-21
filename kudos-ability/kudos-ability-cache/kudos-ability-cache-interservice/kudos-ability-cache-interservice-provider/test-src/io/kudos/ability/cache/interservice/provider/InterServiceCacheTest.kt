package io.kudos.ability.cache.interservice.provider

import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Import


/**
 * 服务间缓存测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableFeignClients
@Import(MockProvider::class)
open class InterServiceCacheTest {

    @Autowired
    private lateinit var proxy: IMockProxy

    @Test
    fun same() {
        val obj1 = proxy.same()
        val obj2 = proxy.same()
        assert(obj1 === obj2)
    }

    @Test
    fun different1() {
        val obj1 = proxy.different1()
        val obj2 = proxy.different1()
        assert(obj1 !== obj2)
    }

    @Test
    fun different2() {
        val obj1 = proxy.different2()
        val obj2 = proxy.different2()
        assert(obj1 !== obj2)
    }

}