package io.kudos.ability.comm.sms.aws.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SmsAwsProxyProperties（純單元測試）
 *
 * 覆蓋：全部屬性的默認值（enable=false、其餘 null）與賦值讀回（含空字符串與 null 重置）。
 *
 * @author K
 * @since 1.0.0
 */
internal class SmsAwsProxyPropertiesTest {

    @Test
    fun defaults() {
        val properties = SmsAwsProxyProperties()

        assertFalse(properties.enable)
        assertNull(properties.url)
        assertNull(properties.username)
        assertNull(properties.password)
    }

    @Test
    fun acceptsAssignedValues() {
        val properties = SmsAwsProxyProperties().apply {
            enable = true
            url = "http://proxy.example.com:1234"
            username = "user"
            password = "p@ss"
        }

        assertTrue(properties.enable)
        assertEquals("http://proxy.example.com:1234", properties.url)
        assertEquals("user", properties.username)
        assertEquals("p@ss", properties.password)

        // 重置回 null / 空串
        properties.url = ""
        assertEquals("", properties.url)
        properties.username = null
        assertNull(properties.username)
        properties.enable = false
        assertFalse(properties.enable)
    }

}
