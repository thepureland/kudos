package io.kudos.context.kit

import io.kudos.context.support.AnotherTestBean
import io.kudos.context.support.ITestBean
import io.kudos.context.support.TestBean
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@EnableKudosTest
@Import(TestBean::class, AnotherTestBean::class)
internal open class SpringKitTest {

    @Test
    fun getBeanByName() {
        assertNotNull(SpringKit.getBean("testBean"))
    }

    @Test
    fun getBeanByType() {
        assertNotNull(SpringKit.getBean<AnotherTestBean>())
    }

    @Test
    fun getBeanOrNullByName() {
        assertNotNull(SpringKit.getBeanOrNull("testBean"))
        assertNull(SpringKit.getBeanOrNull("noSuchBeanName"), "a missing bean name should yield null, not an exception")
    }

    @Test
    fun getBeanOrNullByType() {
        assertNotNull(SpringKit.getBeanOrNull(AnotherTestBean::class))
        assertNull(
            SpringKit.getBeanOrNull(java.math.BigDecimal::class),
            "a type with no bean registered should yield null, not an exception"
        )
    }

    @Test
    fun getProperty() {
        assertEquals("true", SpringKit.getProperty("kudos.context.test"))
        assertNull(SpringKit.getProperty("kudos.context.absent.property"))
    }

    @Test
    fun getBeansOfType() {
        assertEquals(2, SpringKit.getBeansOfType<ITestBean>().size)
    }
}