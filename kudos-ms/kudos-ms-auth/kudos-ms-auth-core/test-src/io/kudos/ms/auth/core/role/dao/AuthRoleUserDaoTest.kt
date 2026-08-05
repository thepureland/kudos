package io.kudos.ms.auth.core.role.dao

import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleUserDao
 *
 * Test data source: `AuthRoleUserDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleUserDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Test
    fun exists() {
        // Test an existing relation
        assertTrue(authRoleUserDao.exists("42d84639-0000-0000-0000-000000000052", "42d84639-0000-0000-0000-000000000050"))

        // Test a non-existent relation
        assertFalse(authRoleUserDao.exists("42d84639-0000-0000-0000-000000000052", "non-existent-user"))
        assertFalse(authRoleUserDao.exists("non-existent-role", "42d84639-0000-0000-0000-000000000050"))
    }

    /**
     * A grant carries delegation and revocation state, not just the (role, user) pair. This pins the
     * mapping of those columns; the behaviour built on them lands with the delegation feature.
     */
    @Test
    fun grantGovernanceColumns_roundTrip() {
        val parentId = authRoleUserDao.insert(AuthRoleUser.Companion().apply {
            this.roleId = "42d84639-0000-0000-0000-000000000052"
            this.userId = "gov-upstream-user"
            this.delegableDepth = 2
        })
        val childId = authRoleUserDao.insert(AuthRoleUser.Companion().apply {
            this.roleId = "42d84639-0000-0000-0000-000000000052"
            this.userId = "gov-downstream-user"
            this.principalType = "SERVICE"
            this.grantedBy = "gov-upstream-user"
            this.parentGrantId = parentId
            this.delegableDepth = 1
            this.scopeSnapshot = """{"org":["org-1"]}"""
            this.revoked = false
            this.revokeReason = null
        })
        try {
            val child = authRoleUserDao.get(childId)!!
            assertEquals("SERVICE", child.principalType)
            assertEquals("gov-upstream-user", child.grantedBy)
            assertEquals(parentId, child.parentGrantId, "the chain edge must survive the round trip")
            assertEquals(1, child.delegableDepth, "the delegated grant narrows the parent's depth of 2")
            assertEquals("""{"org":["org-1"]}""", child.scopeSnapshot)
            assertEquals(false, child.revoked)

            // Defaults must degrade to the pre-delegation behaviour: a plain grant is terminal and live.
            val parent = authRoleUserDao.get(parentId)!!
            assertEquals("USER", parent.principalType)
            assertEquals(false, parent.revoked)
            assertNull(parent.parentGrantId)
        } finally {
            authRoleUserDao.deleteById(childId)
            authRoleUserDao.deleteById(parentId)
        }
    }
}