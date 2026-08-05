package io.kudos.ms.user.core.account.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for UserAccountProtectionDao — loads the fixture protection row through the inherited CRUD
 * `get`, reading every mapped column so the table-binding object and PO accessors are exercised; also
 * covers the not-found path.
 *
 * Test data source: `UserAccountProtectionDaoTest.sql`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountProtectionDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userAccountProtectionDao: UserAccountProtectionDao

    @Test
    fun getLoadsAllMappedColumns() {
        val po = userAccountProtectionDao.get("66666666-0000-0000-0000-000000000101")
        assertNotNull(po)
        assertEquals("66666666-0000-0000-0000-000000000001", po.userId)
        assertEquals("q1", po.question1)
        assertEquals("a1", po.answer1)
        assertEquals("q2", po.question2)
        assertEquals("a2", po.answer2)
        assertEquals("q3", po.question3)
        assertEquals("a3", po.answer3)
        assertNull(po.safeContactWayId)
        assertEquals(2, po.totalValidateCount)
        assertEquals(1, po.matchQuestionCount)
        assertEquals(0, po.errorTimes)
        assertEquals(true, po.active)
        assertEquals(false, po.builtIn)
    }

    @Test
    fun getReturnsNullWhenMissing() {
        assertNull(userAccountProtectionDao.get("no-such-protection-id"))
    }
}
