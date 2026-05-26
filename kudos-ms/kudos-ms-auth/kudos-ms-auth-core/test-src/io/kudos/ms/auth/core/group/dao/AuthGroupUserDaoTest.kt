package io.kudos.ms.auth.core.group.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthGroupUserDao
 *
 * Test data source: `AuthGroupUserDaoTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGroupUserDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Test
    fun exists() {
        // Test existing relation
        assertTrue(authGroupUserDao.exists("a1b2c3d4-0000-0000-0000-000000000072", "a1b2c3d4-0000-0000-0000-000000000070"))

        // Test non-existing relation
        assertFalse(authGroupUserDao.exists("a1b2c3d4-0000-0000-0000-000000000072", "non-existent-user"))
        assertFalse(authGroupUserDao.exists("non-existent-group", "a1b2c3d4-0000-0000-0000-000000000070"))
    }

    @Test
    fun searchUserIdsByGroupId() {
        val groupId = "a1b2c3d4-0000-0000-0000-000000000072"
        val userIds = authGroupUserDao.searchUserIdsByGroupId(groupId)
        assertTrue(userIds.size >= 2)
        assertTrue(userIds.contains("a1b2c3d4-0000-0000-0000-000000000070"))
        assertTrue(userIds.contains("a1b2c3d4-0000-0000-0000-000000000071"))

        // Test non-existing group ID
        val emptyUserIds = authGroupUserDao.searchUserIdsByGroupId("non-existent-group")
        assertTrue(emptyUserIds.isEmpty())
    }

    @Test
    fun searchGroupIdsByUserId() {
        val userId = "a1b2c3d4-0000-0000-0000-000000000070"
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        assertTrue(groupIds.isNotEmpty())
        assertTrue(groupIds.contains("a1b2c3d4-0000-0000-0000-000000000072"))

        // Test non-existing user ID
        val emptyGroupIds = authGroupUserDao.searchGroupIdsByUserId("non-existent-user")
        assertTrue(emptyGroupIds.isEmpty())
    }
}
