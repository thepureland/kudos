package io.kudos.ms.sys.common.datasource.vo.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDataSourceTestRequest
 *
 * Covers property accessors, nullable password and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceTestRequestTest {

    @Test
    fun properties() {
        val req = SysDataSourceTestRequest(
            url = "jdbc:mysql://localhost:3306/db",
            username = "root",
            password = "pwd",
        )
        assertEquals("jdbc:mysql://localhost:3306/db", req.url)
        assertEquals("root", req.username)
        assertEquals("pwd", req.password)
    }

    @Test
    fun nullablePassword() {
        val req = SysDataSourceTestRequest("jdbc:h2:mem:test", "sa", null)
        assertNull(req.password)
    }

    @Test
    fun dataClassContract() {
        val a = SysDataSourceTestRequest("jdbc:h2:mem:test", "sa", null)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(username = "admin"))
        assertTrue(a.toString().contains("sa"))
    }

}
