package io.kudos.ms.sys.common.datasource.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysDataSourceFormUpdate
 *
 * Covers property accessors (id + ISysDataSourceFormBase overrides), IIdEntity contract,
 * nullable fields and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceFormUpdateTest {

    private fun form() = SysDataSourceFormUpdate(
        id = "ds-1",
        name = "ds1",
        subSystemCode = "sys",
        microServiceCode = "ms",
        tenantId = "t-1",
        url = "jdbc:mysql://localhost:3306/db",
        username = "root",
        password = "pwd",
        initialSize = 1,
        maxActive = 10,
        maxIdle = 8,
        minIdle = 2,
        maxWait = 1000,
        maxAge = 60000,
        remark = "r",
    )

    @Test
    fun properties() {
        val f = form()
        assertTrue(f is IIdEntity<*>)
        assertEquals("ds-1", f.id)
        assertEquals("ds1", f.name)
        assertEquals("jdbc:mysql://localhost:3306/db", f.url)
        assertEquals("pwd", f.password)
        assertEquals(60000, f.maxAge)
    }

    @Test
    fun nullableFields() {
        val f = form().copy(password = null, maxAge = null, remark = null, tenantId = null)
        assertNull(f.password)
        assertNull(f.maxAge)
        assertNull(f.remark)
        assertNull(f.tenantId)
    }

    @Test
    fun dataClassContract() {
        val a = form()
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "ds-2"))
        assertTrue(a.toString().contains("ds-1"))
    }

}
