package io.kudos.base.support

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * PropertiesLoader测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class PropertiesLoaderTest {

    @Test
    fun testConstructorWithProperties() {
        val props = Properties()
        props.setProperty("key1", "value1")
        props.setProperty("key2", "value2")
        val loader = PropertiesLoader(props)
        assertEquals("value1", loader.getProperty("key1"))
        assertEquals("value2", loader.getProperty("key2"))
    }

    @Test
    fun testGetProperty() {
        val props = Properties()
        props.setProperty("test.key", "test.value")
        val loader = PropertiesLoader(props)
        assertEquals("test.value", loader.getProperty("test.key"))
    }

    @Test
    fun testGetPropertyWithDefault() {
        val props = Properties()
        val loader = PropertiesLoader(props)
        assertEquals("default", loader.getProperty("nonexistent", "default"))
    }

    @Test
    fun testGetPropertyReturnsNull() {
        val props = Properties()
        val loader = PropertiesLoader(props)
        assertNull(loader.getProperty("nonexistent"))
    }

    @Test
    fun testGetInt() {
        val props = Properties()
        props.setProperty("int.key", "42")
        val loader = PropertiesLoader(props)
        assertEquals(42, loader.getInt("int.key"))
    }

    @Test
    fun testGetIntWithDefault() {
        val props = Properties()
        val loader = PropertiesLoader(props)
        assertEquals(100, loader.getInt("nonexistent", 100))
    }

    @Test
    fun testGetIntInvalidValue() {
        val props = Properties()
        props.setProperty("invalid.int", "not.a.number")
        val loader = PropertiesLoader(props)
        assertFailsWith<NumberFormatException> {
            loader.getInt("invalid.int")
        }
    }

    @Test
    fun testGetIntNull() {
        val props = Properties()
        val loader = PropertiesLoader(props)
        assertNull(loader.getInt("nonexistent"))
    }

    @Test
    fun testGetDouble() {
        val props = Properties()
        props.setProperty("double.key", "3.14")
        val loader = PropertiesLoader(props)
        assertEquals(3.14, loader.getDouble("double.key"))
    }

    @Test
    fun testGetDoubleWithDefault() {
        val props = Properties()
        val loader = PropertiesLoader(props)
        assertEquals(99.99, loader.getDouble("nonexistent", 99.99))
    }

    @Test
    fun testGetDoubleInvalidValue() {
        val props = Properties()
        props.setProperty("invalid.double", "not.a.number")
        val loader = PropertiesLoader(props)
        assertFailsWith<NumberFormatException> {
            loader.getDouble("invalid.double")
        }
    }

    @Test
    fun testGetBoolean() {
        val props = Properties()
        props.setProperty("bool.true", "true")
        props.setProperty("bool.false", "false")
        val loader = PropertiesLoader(props)
        assertEquals(true, loader.getBoolean("bool.true"))
        assertEquals(false, loader.getBoolean("bool.false"))
    }

    @Test
    fun testGetBooleanWithDefault() {
        val props = Properties()
        val loader = PropertiesLoader(props)
        assertEquals(true, loader.getBoolean("nonexistent", true))
        assertEquals(false, loader.getBoolean("nonexistent", false))
    }

    @Test
    fun testGetBooleanInvalidValue() {
        val props = Properties()
        props.setProperty("invalid.bool", "maybe")
        val loader = PropertiesLoader(props)
        // 根据实现，非true/false应该返回false
        assertEquals(false, loader.getBoolean("invalid.bool"))
    }

    @Test
    fun testSystemPropertyPriority() {
        val props = Properties()
        props.setProperty("test.key", "file.value")
        val loader = PropertiesLoader(props)
        
        // 设置系统属性
        val originalValue = System.getProperty("test.key")
        try {
            System.setProperty("test.key", "system.value")
            // 系统属性应该优先
            assertEquals("system.value", loader.getProperty("test.key"))
        } finally {
            if (originalValue != null) {
                System.setProperty("test.key", originalValue)
            } else {
                System.clearProperty("test.key")
            }
        }
    }

    @Test
    fun testSystemPropertyPriorityForInt() {
        val props = Properties()
        props.setProperty("test.int", "100")
        val loader = PropertiesLoader(props)
        
        val originalValue = System.getProperty("test.int")
        try {
            System.setProperty("test.int", "200")
            assertEquals(200, loader.getInt("test.int"))
        } finally {
            if (originalValue != null) {
                System.setProperty("test.int", originalValue)
            } else {
                System.clearProperty("test.int")
            }
        }
    }

    @Test
    fun testNegativeInt() {
        val props = Properties()
        props.setProperty("negative.int", "-42")
        val loader = PropertiesLoader(props)
        assertEquals(-42, loader.getInt("negative.int"))
    }

    @Test
    fun testNegativeDouble() {
        val props = Properties()
        props.setProperty("negative.double", "-3.14")
        val loader = PropertiesLoader(props)
        assertEquals(-3.14, loader.getDouble("negative.double"))
    }

    @Test
    fun testEmptyStringProperty() {
        val props = Properties()
        props.setProperty("empty.key", "")
        val loader = PropertiesLoader(props)
        assertEquals("", loader.getProperty("empty.key"))
    }

    @Test
    fun testPropertiesAccess() {
        val props = Properties()
        props.setProperty("key1", "value1")
        val loader = PropertiesLoader(props)
        val loadedProps = loader.properties
        assertEquals("value1", loadedProps.getProperty("key1"))
    }
}
