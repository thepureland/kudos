package io.kudos.ability.cache.interservice.provider

import io.kudos.context.kit.SpringKit
import io.kudos.test.common.SpringTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.openfeign.EnableFeignClients
import java.io.Closeable


@EnableFeignClients
//@ComponentScan(basePackages = [
//    "io.kudos.ability.cache.interservice.provider",
//], excludeFilters = [
//    ComponentScan.Filter(
//        type = FilterType.ASSIGNABLE_TYPE,
//        classes = [ProviderApplication::class]
//    ),
//    ComponentScan.Filter(
//        type = FilterType.ASSIGNABLE_TYPE,
//        classes = [MockProvider::class]
//    ),
//])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class InterServiceCacheTest :SpringTest() {

    @Autowired
    private lateinit var proxy: IMockProxy

    @BeforeAll
    open fun startupProvider() {
        ProviderApplication.main(emptyArray())
    }

    @AfterAll
    open fun shutdownProvider() {
        (SpringKit.getApplicationContext() as Closeable).close()
    }


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