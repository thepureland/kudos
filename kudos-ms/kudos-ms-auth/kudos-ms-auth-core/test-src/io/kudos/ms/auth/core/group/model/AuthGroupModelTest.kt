package io.kudos.ms.auth.core.group.model

import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupRole
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.group.model.table.AuthGroupRoles
import io.kudos.ms.auth.core.group.model.table.AuthGroupUsers
import io.kudos.ms.auth.core.group.model.table.AuthGroups
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pure unit test for the group POs (Ktorm entity proxies) and their table mapping objects.
 *
 * - PO tests exercise each property's getter/setter round-trip on a freshly created Ktorm entity,
 *   including nullable fields set to null. No DB or Spring context is required — the entity is a
 *   plain in-memory proxy produced by the companion factory.
 * - Table tests pin down the physical table name and every column's name + entity binding so an
 *   accidental rename in either the PO or the schema is caught.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupModelTest {

    private val ts = LocalDateTime.of(2025, 6, 15, 10, 30, 0)

    @Test
    fun authGroup_propertyRoundTrip() {
        val g = AuthGroup {
            id = "g1"
            code = "GRP_ADMIN"
            name = "Admins"
            tenantId = "t1"
            subsysCode = "ams"
            remark = "remark"
            active = true
            builtIn = false
            createUserId = "cu"
            createUserName = "creator"
            createTime = ts
            updateUserId = "uu"
            updateUserName = "updater"
            updateTime = ts
        }
        assertEquals("g1", g.id)
        assertEquals("GRP_ADMIN", g.code)
        assertEquals("Admins", g.name)
        assertEquals("t1", g.tenantId)
        assertEquals("ams", g.subsysCode)
        assertEquals("remark", g.remark)
        assertEquals(true, g.active)
        assertEquals(false, g.builtIn)
        assertEquals("cu", g.createUserId)
        assertEquals("creator", g.createUserName)
        assertEquals(ts, g.createTime)
        assertEquals("uu", g.updateUserId)
        assertEquals("updater", g.updateUserName)
        assertEquals(ts, g.updateTime)
    }

    @Test
    fun authGroup_nullableFieldsAcceptNull() {
        val g = AuthGroup {
            code = "C"
            name = "N"
            tenantId = "t"
            subsysCode = "s"
            active = true
            builtIn = false
            remark = null
            createUserId = null
            createUserName = null
            createTime = null
            updateUserId = null
            updateUserName = null
            updateTime = null
        }
        assertNull(g.remark)
        assertNull(g.createUserId)
        assertNull(g.createUserName)
        assertNull(g.createTime)
        assertNull(g.updateUserId)
        assertNull(g.updateUserName)
        assertNull(g.updateTime)
    }

    @Test
    fun authGroupUser_propertyRoundTrip() {
        val gu = AuthGroupUser {
            id = "gu1"
            groupId = "g1"
            userId = "u1"
            createUserId = "cu"
            createUserName = "creator"
            createTime = ts
            updateUserId = "uu"
            updateUserName = "updater"
            updateTime = ts
        }
        assertEquals("gu1", gu.id)
        assertEquals("g1", gu.groupId)
        assertEquals("u1", gu.userId)
        assertEquals("cu", gu.createUserId)
        assertEquals("creator", gu.createUserName)
        assertEquals(ts, gu.createTime)
        assertEquals("uu", gu.updateUserId)
        assertEquals("updater", gu.updateUserName)
        assertEquals(ts, gu.updateTime)
        // nullable audit fields may be null
        val bare = AuthGroupUser { groupId = "g2"; userId = "u2"; createTime = null }
        assertNull(bare.createTime)
        assertNull(bare.createUserId)
    }

    @Test
    fun authGroupRole_propertyRoundTrip() {
        val gr = AuthGroupRole {
            id = "gr1"
            groupId = "g1"
            roleId = "r1"
            createUserId = "cu"
            createUserName = "creator"
            createTime = ts
            updateUserId = "uu"
            updateUserName = "updater"
            updateTime = ts
        }
        assertEquals("gr1", gr.id)
        assertEquals("g1", gr.groupId)
        assertEquals("r1", gr.roleId)
        assertEquals("cu", gr.createUserId)
        assertEquals("creator", gr.createUserName)
        assertEquals(ts, gr.createTime)
        assertEquals("uu", gr.updateUserId)
        assertEquals("updater", gr.updateUserName)
        assertEquals(ts, gr.updateTime)
        val bare = AuthGroupRole { groupId = "g2"; roleId = "r2"; updateTime = null }
        assertNull(bare.updateTime)
        assertNull(bare.updateUserName)
    }

    @Test
    fun authGroups_tableMapping() {
        assertEquals("auth_group", AuthGroups.tableName)
        assertEquals("id", AuthGroups.id.name)
        assertEquals("code", AuthGroups.code.name)
        assertEquals("name", AuthGroups.name.name)
        assertEquals("tenant_id", AuthGroups.tenantId.name)
        assertEquals("subsys_code", AuthGroups.subsysCode.name)
        assertEquals("remark", AuthGroups.remark.name)
        assertEquals("active", AuthGroups.active.name)
        assertEquals("built_in", AuthGroups.builtIn.name)
        assertEquals("create_user_id", AuthGroups.createUserId.name)
        assertEquals("create_user_name", AuthGroups.createUserName.name)
        assertEquals("create_time", AuthGroups.createTime.name)
        assertEquals("update_user_id", AuthGroups.updateUserId.name)
        assertEquals("update_user_name", AuthGroups.updateUserName.name)
        assertEquals("update_time", AuthGroups.updateTime.name)
    }

    @Test
    fun authGroupUsers_tableMapping() {
        assertEquals("auth_group_user", AuthGroupUsers.tableName)
        assertEquals("id", AuthGroupUsers.id.name)
        assertEquals("group_id", AuthGroupUsers.groupId.name)
        assertEquals("user_id", AuthGroupUsers.userId.name)
        assertEquals("create_user_id", AuthGroupUsers.createUserId.name)
        assertEquals("create_user_name", AuthGroupUsers.createUserName.name)
        assertEquals("create_time", AuthGroupUsers.createTime.name)
        assertEquals("update_user_id", AuthGroupUsers.updateUserId.name)
        assertEquals("update_user_name", AuthGroupUsers.updateUserName.name)
        assertEquals("update_time", AuthGroupUsers.updateTime.name)
    }

    @Test
    fun authGroupRoles_tableMapping() {
        assertEquals("auth_group_role", AuthGroupRoles.tableName)
        assertEquals("id", AuthGroupRoles.id.name)
        assertEquals("group_id", AuthGroupRoles.groupId.name)
        assertEquals("role_id", AuthGroupRoles.roleId.name)
        assertEquals("create_user_id", AuthGroupRoles.createUserId.name)
        assertEquals("create_user_name", AuthGroupRoles.createUserName.name)
        assertEquals("create_time", AuthGroupRoles.createTime.name)
        assertEquals("update_user_id", AuthGroupRoles.updateUserId.name)
        assertEquals("update_user_name", AuthGroupRoles.updateUserName.name)
        assertEquals("update_time", AuthGroupRoles.updateTime.name)
    }
}
