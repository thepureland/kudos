package io.kudos.ms.user.common.org.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserOrgFormCreate / UserOrgFormUpdate
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgFormsTest {

    private fun create() = UserOrgFormCreate(
        name = "HQ",
        shortName = "hq",
        tenantId = "t-1",
        parentId = null,
        orgTypeDictCode = "1",
        sortNum = 1,
        remark = "root",
    )

    private fun update() = UserOrgFormUpdate(
        id = "o-1",
        name = "HQ",
        shortName = "hq",
        tenantId = "t-1",
        parentId = null,
        orgTypeDictCode = "1",
        sortNum = 1,
        remark = "root",
    )

    @Test
    fun createProperties() {
        val c = create()
        assertEquals("HQ", c.name)
        assertEquals(1, c.sortNum)
        assertNull(c.parentId)
        val base: IUserOrgFormBase = c
        assertEquals("hq", base.shortName)
    }

    @Test
    fun createDataClassContract() {
        val a = create()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(name = "Branch"))
        assertTrue(a.toString().contains("HQ"))
    }

    @Test
    fun updateProperties() {
        val u = update()
        assertEquals("o-1", u.id)
        val asEntity: IIdEntity<String> = u
        assertEquals("o-1", asEntity.id)
        assertEquals("HQ", u.name)
    }

    @Test
    fun updateDataClassContract() {
        val a = update()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(id = "o-2"))
        assertTrue(a.toString().contains("o-1"))
    }

}
