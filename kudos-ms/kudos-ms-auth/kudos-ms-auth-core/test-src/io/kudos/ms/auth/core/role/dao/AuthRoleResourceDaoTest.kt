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
 * junit test for [AuthRoleResourceDao].
 *
 * Covers exists, searchRoleIdsByResourceId, searchResourceIdsByRoleIds (empty-input early return +
 * dedup across roles), searchAllRoleIdToResourceIdsForCache, and both delete paths
 * (deleteByRoleIdAndResourceId, deleteByRoleId). Delete cases run inside the test transaction and
 * are rolled back, so the fixture is restored for the next method.
 *
 * Test data source: `AuthRoleResourceDaoTest.sql`
 *  - role60 -> {res62, res63}; role61 -> {res62}
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

    private val role60 = "49748162-0000-0000-0000-000000000060"
    private val role61 = "49748162-0000-0000-0000-000000000061"
    private val res62 = "49748162-0000-0000-0000-000000000062"
    private val res63 = "49748162-0000-0000-0000-000000000063"

    @Test
    fun exists() {
        // Test an existing relation
        assertTrue(authRoleResourceDao.exists(role60, res62))

        // Test a non-existent relation
        assertFalse(authRoleResourceDao.exists(role60, "non-existent-resource"))
        assertFalse(authRoleResourceDao.exists("non-existent-role", res62))
    }

    @Test
    fun searchRoleIdsByResourceId() {
        val roleIds = authRoleResourceDao.searchRoleIdsByResourceId(res62)
        assertEquals(setOf(role60, role61), roleIds, "res62 is granted to both role60 and role61")

        // res63 is granted only to role60
        assertEquals(setOf(role60), authRoleResourceDao.searchRoleIdsByResourceId(res63))

        // Test a non-existent resource ID
        assertTrue(authRoleResourceDao.searchRoleIdsByResourceId("non-existent-resource").isEmpty())
    }

    @Test
    fun searchResourceIdsByRoleIds_emptyInputReturnsEmpty() {
        assertTrue(authRoleResourceDao.searchResourceIdsByRoleIds(emptyList()).isEmpty())
    }

    @Test
    fun searchResourceIdsByRoleIds_dedupAcrossRoles() {
        // role60 -> {62,63}, role61 -> {62}; union deduplicated = {62,63}
        val ids = authRoleResourceDao.searchResourceIdsByRoleIds(listOf(role60, role61))
        assertEquals(setOf(res62, res63), ids)
        // single role
        assertEquals(setOf(res62), authRoleResourceDao.searchResourceIdsByRoleIds(listOf(role61)))
        // non-existent role -> empty
        assertTrue(authRoleResourceDao.searchResourceIdsByRoleIds(listOf("non-existent")).isEmpty())
    }

    @Test
    fun searchAllRoleIdToResourceIdsForCache() {
        val map = authRoleResourceDao.searchAllRoleIdToResourceIdsForCache()
        assertEquals(setOf(res62, res63), map[role60]?.toSet())
        assertEquals(listOf(res62), map[role61])
    }

    @Test
    fun deleteByRoleIdAndResourceId() {
        assertTrue(authRoleResourceDao.exists(role60, res63))
        val deleted = authRoleResourceDao.deleteByRoleIdAndResourceId(role60, res63)
        assertEquals(1, deleted)
        assertFalse(authRoleResourceDao.exists(role60, res63))
        // the other grant of role60 survives
        assertTrue(authRoleResourceDao.exists(role60, res62))

        // deleting a non-existent relation removes nothing
        assertEquals(0, authRoleResourceDao.deleteByRoleIdAndResourceId(role60, "non-existent-resource"))
    }

    @Test
    fun deleteByRoleId() {
        // role60 has two grants
        val deleted = authRoleResourceDao.deleteByRoleId(role60)
        assertEquals(2, deleted)
        assertTrue(authRoleResourceDao.searchResourceIdsByRoleIds(listOf(role60)).isEmpty())
        // role61's grant is untouched
        assertTrue(authRoleResourceDao.exists(role61, res62))

        // deleting for a role with no grants removes nothing
        assertEquals(0, authRoleResourceDao.deleteByRoleId("non-existent-role"))
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
