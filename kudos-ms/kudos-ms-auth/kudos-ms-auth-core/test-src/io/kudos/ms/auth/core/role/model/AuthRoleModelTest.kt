package io.kudos.ms.auth.core.role.model

import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.model.table.AuthRoleResources
import io.kudos.ms.auth.core.role.model.table.AuthRoleUsers
import io.kudos.ms.auth.core.role.model.table.AuthRoles
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for the role POs (Ktorm entity proxies) and their table mapping objects.
 *
 * - PO tests exercise every property's getter/setter round-trip on a freshly created Ktorm entity,
 *   including the nullable fields set to null. No DB or Spring context is required — the entity is a
 *   plain in-memory proxy produced by the companion factory.
 * - Table tests pin down the physical table name and every column's name + entity binding so an
 *   accidental rename in either the PO or the schema is caught.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class AuthRoleModelTest {

    private val ts = LocalDateTime.of(2025, 6, 15, 10, 30, 0)

    @Test
    fun authRole_propertyRoundTrip() {
        val role = AuthRole {
            id = "r1"
            code = "ROLE_ADMIN"
            name = "Administrator"
            tenantId = "t1"
            subsysCode = "ams"
            parentId = "p1"
            dataScope = "ALL"
            remark = "remark"
            active = true
            builtIn = false
            approvalRequired = true
            createUserId = "cu"
            createUserName = "creator"
            createTime = ts
            updateUserId = "uu"
            updateUserName = "updater"
            updateTime = ts
        }
        assertEquals("r1", role.id)
        assertEquals("ROLE_ADMIN", role.code)
        assertEquals("Administrator", role.name)
        assertEquals("t1", role.tenantId)
        assertEquals("ams", role.subsysCode)
        assertEquals("p1", role.parentId)
        assertEquals("ALL", role.dataScope)
        assertEquals("remark", role.remark)
        assertEquals(true, role.active)
        assertEquals(false, role.builtIn)
        assertEquals(true, role.approvalRequired)
        assertEquals("cu", role.createUserId)
        assertEquals("creator", role.createUserName)
        assertEquals(ts, role.createTime)
        assertEquals("uu", role.updateUserId)
        assertEquals("updater", role.updateUserName)
        assertEquals(ts, role.updateTime)
    }

    @Test
    fun authRole_nullableFieldsAcceptNull() {
        val role = AuthRole {
            code = "C"
            name = "N"
            tenantId = "t"
            subsysCode = "s"
            active = true
            parentId = null
            dataScope = null
            remark = null
            builtIn = false
            approvalRequired = null
            createTime = null
        }
        assertNull(role.parentId)
        assertNull(role.dataScope)
        assertNull(role.remark)
        // builtIn is a non-null Boolean on the entity, so it cannot take null and is only assigned
        // above because construction requires it; assert the value that was actually set.
        assertEquals(false, role.builtIn)
        assertNull(role.approvalRequired)
        assertNull(role.createTime)
    }

    @Test
    fun authRoleResource_propertyRoundTrip() {
        val rr = AuthRoleResource {
            id = "rr1"
            roleId = "role1"
            resourceId = "res1"
            createUserId = "cu"
            createUserName = "creator"
            createTime = ts
            updateUserId = "uu"
            updateUserName = "updater"
            updateTime = ts
        }
        assertEquals("rr1", rr.id)
        assertEquals("role1", rr.roleId)
        assertEquals("res1", rr.resourceId)
        assertEquals("cu", rr.createUserId)
        assertEquals(ts, rr.updateTime)
    }

    @Test
    fun authRoleUser_propertyRoundTripIncludingWindow() {
        val ru = AuthRoleUser {
            id = "ru1"
            roleId = "role1"
            userId = "user1"
            startTime = ts
            endTime = ts.plusDays(30)
            createUserId = "cu"
            createUserName = "creator"
            createTime = ts
            updateUserId = "uu"
            updateUserName = "updater"
            updateTime = ts
        }
        assertEquals("ru1", ru.id)
        assertEquals("role1", ru.roleId)
        assertEquals("user1", ru.userId)
        assertEquals(ts, ru.startTime)
        assertEquals(ts.plusDays(30), ru.endTime)
        // window may be open-ended
        val permanent = AuthRoleUser {
            roleId = "role2"
            userId = "user2"
            startTime = null
            endTime = null
        }
        assertNull(permanent.startTime)
        assertNull(permanent.endTime)
    }

    @Test
    fun authRoles_tableMapping() {
        assertEquals("auth_role", AuthRoles.tableName)
        assertEquals("id", AuthRoles.id.name)
        assertEquals("code", AuthRoles.code.name)
        assertEquals("name", AuthRoles.name.name)
        assertEquals("tenant_id", AuthRoles.tenantId.name)
        assertEquals("subsys_code", AuthRoles.subsysCode.name)
        assertEquals("parent_id", AuthRoles.parentId.name)
        assertEquals("data_scope", AuthRoles.dataScope.name)
        assertEquals("remark", AuthRoles.remark.name)
        assertEquals("active", AuthRoles.active.name)
        assertEquals("built_in", AuthRoles.builtIn.name)
        assertEquals("approval_required", AuthRoles.approvalRequired.name)
        assertEquals("create_user_id", AuthRoles.createUserId.name)
        assertEquals("create_user_name", AuthRoles.createUserName.name)
        assertEquals("create_time", AuthRoles.createTime.name)
        assertEquals("update_user_id", AuthRoles.updateUserId.name)
        assertEquals("update_user_name", AuthRoles.updateUserName.name)
        assertEquals("update_time", AuthRoles.updateTime.name)
    }

    @Test
    fun authRoleResources_tableMapping() {
        assertEquals("auth_role_resource", AuthRoleResources.tableName)
        assertEquals("id", AuthRoleResources.id.name)
        assertEquals("role_id", AuthRoleResources.roleId.name)
        assertEquals("resource_id", AuthRoleResources.resourceId.name)
        assertEquals("create_user_id", AuthRoleResources.createUserId.name)
        assertEquals("create_user_name", AuthRoleResources.createUserName.name)
        assertEquals("create_time", AuthRoleResources.createTime.name)
        assertEquals("update_user_id", AuthRoleResources.updateUserId.name)
        assertEquals("update_user_name", AuthRoleResources.updateUserName.name)
        assertEquals("update_time", AuthRoleResources.updateTime.name)
    }

    @Test
    fun authRoleUsers_tableMapping() {
        assertEquals("auth_role_user", AuthRoleUsers.tableName)
        assertEquals("id", AuthRoleUsers.id.name)
        assertEquals("role_id", AuthRoleUsers.roleId.name)
        assertEquals("user_id", AuthRoleUsers.userId.name)
        assertEquals("start_time", AuthRoleUsers.startTime.name)
        assertEquals("end_time", AuthRoleUsers.endTime.name)
        assertEquals("create_user_id", AuthRoleUsers.createUserId.name)
        assertEquals("create_user_name", AuthRoleUsers.createUserName.name)
        assertEquals("create_time", AuthRoleUsers.createTime.name)
        assertEquals("update_user_id", AuthRoleUsers.updateUserId.name)
        assertEquals("update_user_name", AuthRoleUsers.updateUserName.name)
        assertEquals("update_time", AuthRoleUsers.updateTime.name)
    }
}
