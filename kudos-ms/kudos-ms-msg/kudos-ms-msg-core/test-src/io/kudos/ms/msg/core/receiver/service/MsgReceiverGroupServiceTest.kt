package io.kudos.ms.msg.core.receiver.service

import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import io.kudos.ms.msg.core.receiver.model.po.MsgReceiverGroup
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiverGroupService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for MsgReceiverGroupService — the cache-entry typed get override, id lookup and the
 * active-group listing with optional type filter.
 *
 * Test data source: `receiver/MsgReceiverGroupServiceTest.sql`.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class MsgReceiverGroupServiceTest : RdbTestBase() {

    @Resource
    private lateinit var msgReceiverGroupService: IMsgReceiverGroupService

    private val userGroupId = "a1000000-0000-0000-0000-000000000001" // USER, active
    private val deptGroupId = "a1000000-0000-0000-0000-000000000002" // DEPT, active
    private val roleGroupId = "a1000000-0000-0000-0000-000000000003" // ROLE, inactive

    @Test
    fun getReceiverGroupByIdReturnsCacheEntry() {
        val entry = msgReceiverGroupService.getReceiverGroupById(userGroupId)
        assertNotNull(entry)
        assertEquals(userGroupId, entry.id)
        assertEquals("USER", entry.receiverGroupTypeDictCode)
        assertEquals(true, entry.active)
    }

    @Test
    fun getReceiverGroupByIdReturnsNullForUnknown() {
        assertNull(msgReceiverGroupService.getReceiverGroupById("no-such-group"))
    }

    @Test
    fun getWithCacheEntryTypeUsesOverrideBranch() {
        // returnType == MsgReceiverGroupCacheEntry -> dao.get(id, returnType)
        val entry = msgReceiverGroupService.get(userGroupId, MsgReceiverGroupCacheEntry::class)
        assertNotNull(entry)
        assertEquals(userGroupId, entry.id)
    }

    @Test
    fun getWithEntityTypeFallsBackToSuper() {
        // returnType != MsgReceiverGroupCacheEntry -> super.get(id, returnType)
        val po = msgReceiverGroupService.get(roleGroupId, MsgReceiverGroup::class)
        assertNotNull(po)
        assertEquals(roleGroupId, po.id)
        assertEquals("ROLE", po.receiverGroupTypeDictCode)
    }

    @Test
    fun listActiveWithNullTypeReturnsAllActive() {
        val groups = msgReceiverGroupService.listActiveReceiverGroups(null)
        val ids = groups.map { it.id }.toSet()
        // both active groups present, the inactive ROLE group excluded
        assertTrue(userGroupId in ids)
        assertTrue(deptGroupId in ids)
        assertTrue(roleGroupId !in ids)
        assertTrue(groups.all { it.active == true })
    }

    @Test
    fun listActiveWithBlankTypeBehavesAsNull() {
        // blank string takes the same null-or-blank branch as null
        val groups = msgReceiverGroupService.listActiveReceiverGroups("   ")
        val ids = groups.map { it.id }.toSet()
        assertTrue(userGroupId in ids)
        assertTrue(deptGroupId in ids)
        assertTrue(roleGroupId !in ids)
    }

    @Test
    fun listActiveWithTypeFiltersByType() {
        val groups = msgReceiverGroupService.listActiveReceiverGroups("USER")
        assertEquals(1, groups.size)
        assertEquals(userGroupId, groups.single().id)
    }

    @Test
    fun listActiveWithInactiveTypeReturnsEmpty() {
        // ROLE exists but is inactive -> active=true filter excludes it
        assertTrue(msgReceiverGroupService.listActiveReceiverGroups("ROLE").isEmpty())
    }

    @Test
    fun listActiveWithUnknownTypeReturnsEmpty() {
        assertTrue(msgReceiverGroupService.listActiveReceiverGroups("NO_SUCH_TYPE").isEmpty())
    }
}
