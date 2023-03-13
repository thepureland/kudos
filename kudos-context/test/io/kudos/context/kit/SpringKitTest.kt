package io.kudos.context.kit

import io.kudos.context.support.AnotherTestBean
import io.kudos.context.support.ITestBean
import io.kudos.test.common.SpringTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal open class SpringKitTest: SpringTest() {

    @Test
    fun getBean() {
        assertNotNull(SpringKit.getBean("testBean"))
    }

    @Test
    fun testGetBean() {
        assertNotNull(SpringKit.getBean(AnotherTestBean::class))
    }

    @Test
    fun getProperty() {
        assertEquals("false", SpringKit.getProperty("spring.cloud.config.enabled"))
    }

    @Test
    fun getBeansOfType() {
        assertEquals(2, SpringKit.getBeansOfType(ITestBean::class).size)
    }
}