package io.kudos.ms.auth.core.group.service

import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupService
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * junit test for AuthGroupService
 *
 * Test data source: `AuthGroupServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGroupServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authGroupService: IAuthGroupService

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Test
    fun deletingGroupRemovesEveryMembershipAndRoleBinding() {
        val groupId = "9a4a2d1b-0000-0000-0000-000000000030"
        authGroupUserDao.insert(AuthGroupUser {
            this.groupId = groupId
            this.userId = "cascade-user"
            this.revoked = false
        })
        authGroupRoleDao.insert(AuthGroupRole {
            this.groupId = groupId
            this.roleId = "cascade-role"
        })

        assertTrue(authGroupService.deleteById(groupId))
        assertTrue(authGroupUserDao.searchMemberUserIdsByGroupId(groupId).isEmpty())
        assertTrue(authGroupRoleDao.searchRoleIdsByGroupId(groupId).isEmpty())
    }

}
