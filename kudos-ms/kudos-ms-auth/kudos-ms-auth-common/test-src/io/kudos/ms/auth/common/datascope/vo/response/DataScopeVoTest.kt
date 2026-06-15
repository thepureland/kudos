package io.kudos.ms.auth.common.datascope.vo.response

import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for DataScopeVo
 *
 * Covers the [DataScopeVo.all] and [DataScopeVo.selfOnly] factory methods, explicit org-scope
 * construction, Serializable marker and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class DataScopeVoTest {

    @Test
    fun allFactory() {
        val vo = DataScopeVo.all()
        assertTrue(vo.all)
        assertFalse(vo.self)
        assertTrue(vo.orgIds.isEmpty())
    }

    @Test
    fun selfOnlyFactory() {
        val vo = DataScopeVo.selfOnly()
        assertFalse(vo.all)
        assertTrue(vo.self)
        assertTrue(vo.orgIds.isEmpty())
    }

    @Test
    fun explicitOrgScope() {
        val vo = DataScopeVo(all = false, self = false, orgIds = setOf("o-1", "o-2"))
        assertFalse(vo.all)
        assertFalse(vo.self)
        assertEquals(setOf("o-1", "o-2"), vo.orgIds)
    }

    @Test
    fun isSerializable() {
        assertTrue(DataScopeVo.all() is Serializable)
    }

    @Test
    fun dataClassContract() {
        val a = DataScopeVo(all = false, self = true, orgIds = setOf("o-1"))
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(orgIds = setOf("o-2")))
        assertNotEquals(a, a.copy(self = false))
        assertNotEquals(DataScopeVo.all(), DataScopeVo.selfOnly())
    }
}
