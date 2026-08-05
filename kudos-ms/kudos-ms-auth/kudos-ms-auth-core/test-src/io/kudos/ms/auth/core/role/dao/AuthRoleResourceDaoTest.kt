package io.kudos.ms.auth.core.role.dao

import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleResourceDao
 *
 * Test data source: `AuthRoleResourceDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleResourceDaoTest : RdbTestBase() {

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Test
    fun exists() {
        // Test an existing relation
        assertTrue(authRoleResourceDao.exists("49748162-0000-0000-0000-000000000060", "49748162-0000-0000-0000-000000000062"))

        // Test a non-existent relation
        assertFalse(authRoleResourceDao.exists("49748162-0000-0000-0000-000000000060", "non-existent-resource"))
        assertFalse(authRoleResourceDao.exists("non-existent-role", "49748162-0000-0000-0000-000000000062"))
    }

    @Test
    fun searchRoleIdsByResourceId() {
        val resourceId = "49748162-0000-0000-0000-000000000062"
        val roleIds = authRoleResourceDao.searchRoleIdsByResourceId(resourceId)
        assertTrue(roleIds.isNotEmpty())
        assertTrue(roleIds.contains("49748162-0000-0000-0000-000000000060"))
        
        // Test a non-existent resource ID
        val emptyRoleIds = authRoleResourceDao.searchRoleIdsByResourceId("non-existent-resource")
        assertTrue(emptyRoleIds.isEmpty())
    }

    /**
     * A binding may address its permission by durable code instead of by resource primary key —
     * including wildcards, a DENY effect and a condition. Resource-id-shaped queries must simply
     * skip such rows rather than choke on the null.
     */
    @Test
    fun codeAddressedBinding_roundTripAndIsSkippedByResourceIdQueries() {
        val roleId = "49748162-0000-0000-0000-000000000060"
        val beforeResourceIds = authRoleResourceDao.searchResourceIdsByRoleIds(listOf(roleId))

        val id = authRoleResourceDao.insert(AuthRoleResource.Companion().apply {
            this.roleId = roleId
            this.permissionCode = "sys:user:*"
            this.effect = "DENY"
            this.condition = """{"ip":"10.0.0.0/8"}"""
        })
        try {
            val row = authRoleResourceDao.get(id)!!
            assertNull(row.resourceId, "a code-addressed binding carries no resource id")
            assertEquals("sys:user:*", row.permissionCode)
            assertEquals("DENY", row.effect)
            assertEquals("""{"ip":"10.0.0.0/8"}""", row.condition)

            assertEquals(
                beforeResourceIds,
                authRoleResourceDao.searchResourceIdsByRoleIds(listOf(roleId)),
                "resource-id resolution must ignore code-addressed rows, not fail on them",
            )
            assertTrue(authRoleResourceDao.searchAllRoleIdToResourceIdsForCache()[roleId].orEmpty().none { it.isBlank() })
        } finally {
            authRoleResourceDao.deleteById(id)
        }
    }

    @Test
    fun resourceIdBinding_defaultsToAllowWithoutCondition() {
        val id = authRoleResourceDao.insert(AuthRoleResource.Companion().apply {
            this.roleId = "49748162-0000-0000-0000-000000000060"
            this.resourceId = "effect-default-probe"
        })
        try {
            val row = authRoleResourceDao.get(id)!!
            assertEquals("ALLOW", row.effect, "existing-style bindings must keep granting, not denying")
            assertNull(row.condition)
            assertNull(row.permissionCode)
        } finally {
            authRoleResourceDao.deleteById(id)
        }
    }
}
