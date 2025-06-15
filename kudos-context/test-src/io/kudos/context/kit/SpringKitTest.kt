package io.kudos.context.kit

import io.kudos.context.support.AnotherTestBean
import io.kudos.context.support.ITestBean
import io.kudos.context.support.TestBean
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@EnableKudosTest
@Import(TestBean::class, AnotherTestBean::class)
internal open class SpringKitTest {

    @Test
    fun getBeanByName() {
        assertNotNull(SpringKit.getBean("testBean"))
    }

    @Test
    fun getBeanByType() {
        assertNotNull(SpringKit.getBean(AnotherTestBean::class))
    }

    @Test
    fun getProperty() {
        assertEquals("true", SpringKit.getProperty("kudos.context.test"))
    }

    @Test
    fun getBeansOfType() {
        assertEquals(2, SpringKit.getBeansOfType(ITestBean::class).size)
    }
}