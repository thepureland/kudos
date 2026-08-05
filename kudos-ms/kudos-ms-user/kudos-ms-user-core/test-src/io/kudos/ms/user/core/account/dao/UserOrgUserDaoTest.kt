package io.kudos.ms.user.core.account.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for UserOrgUserDao — covers `exists` plus all the relation-query and delete helpers
 * (`searchOrgIdsByUserId`, `searchUserIdsByOrgId`, `searchUserIdsByOrgIds` incl. the empty short-circuit,
 * `searchAllUserIdToOrgIds`, `searchAllOrgIdToUserIds`, `searchByOrgIdAndUserId`, `searchAdminUserIdsByOrgId`,
 * `deleteByOrgIdAndUserId`).
 *
 * Test data source: `UserOrgUserDaoTest.sql`.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserOrgUserDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userOrgUserDao: UserOrgUserDao

    private val org1 = "952bb1b3-0000-0000-0000-000000000042"
    private val org2 = "952bb1b3-0000-0000-0000-000000000043"
    private val user1 = "952bb1b3-0000-0000-0000-000000000040"
    private val user2 = "952bb1b3-0000-0000-0000-000000000041"

    @Test
    fun exists() {
        // Test an existing relation.
        assertTrue(userOrgUserDao.exists(org1, user1))

        // Test a non-existent relation.
        assertFalse(userOrgUserDao.exists(org1, "non-existent-user"))
        assertFalse(userOrgUserDao.exists("non-existent-org", user1))
    }

    @Test
    fun searchOrgIdsByUserId() {
        val orgIds = userOrgUserDao.searchOrgIdsByUserId(user1)
        assertTrue(orgIds.contains(org1))
        assertTrue(userOrgUserDao.searchOrgIdsByUserId("non-existent-user").isEmpty())
    }

    @Test
    fun searchUserIdsByOrgId() {
        val userIds = userOrgUserDao.searchUserIdsByOrgId(org1)
        assertTrue(userIds.contains(user1))
        assertTrue(userIds.contains(user2))
        assertTrue(userOrgUserDao.searchUserIdsByOrgId(org2).isEmpty())
    }

    @Test
    fun searchUserIdsByOrgIds() {
        // empty input short-circuits to empty list.
        assertTrue(userOrgUserDao.searchUserIdsByOrgIds(emptyList()).isEmpty())

        val userIds = userOrgUserDao.searchUserIdsByOrgIds(listOf(org1, org2))
        assertTrue(userIds.contains(user1))
        assertTrue(userIds.contains(user2))
    }

    @Test
    fun searchAllUserIdToOrgIds() {
        val map = userOrgUserDao.searchAllUserIdToOrgIds()
        assertTrue(map[user1]?.contains(org1) == true)
    }

    @Test
    fun searchAllOrgIdToUserIds() {
        val map = userOrgUserDao.searchAllOrgIdToUserIds()
        val users = map[org1].orEmpty()
        assertTrue(users.contains(user1))
        assertTrue(users.contains(user2))
    }

    @Test
    fun searchByOrgIdAndUserId() {
        val relations = userOrgUserDao.searchByOrgIdAndUserId(org1, user1)
        assertEquals(1, relations.size)
        assertEquals(org1, relations.first().orgId)
        assertEquals(user1, relations.first().userId)

        assertTrue(userOrgUserDao.searchByOrgIdAndUserId(org1, "non-existent-user").isEmpty())
    }

    @Test
    fun searchAdminUserIdsByOrgId() {
        // user1 is org_admin=true, user2 is false in the fixture.
        val admins = userOrgUserDao.searchAdminUserIdsByOrgId(org1)
        assertTrue(admins.contains(user1))
        assertFalse(admins.contains(user2))
    }

    @Test
    fun deleteByOrgIdAndUserId() {
        // user2 has a non-admin relation in org1; deleting it returns 1, then 0 on the second attempt.
        val deleted = userOrgUserDao.deleteByOrgIdAndUserId(org1, user2)
        assertEquals(1, deleted)
        assertFalse(userOrgUserDao.exists(org1, user2))
        assertEquals(0, userOrgUserDao.deleteByOrgIdAndUserId(org1, user2))
    }
}