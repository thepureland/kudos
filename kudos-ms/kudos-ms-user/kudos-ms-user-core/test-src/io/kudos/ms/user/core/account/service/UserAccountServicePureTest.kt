package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAccountInserted
import io.kudos.ms.user.core.account.event.UserAccountUpdated
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.impl.UserAccountService
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [UserAccountService] — exercises the branch / failure / boundary paths that the
 * Docker-gated [UserAccountServiceTest] integration test cannot reliably reach (DAO update returning
 * false, missing user, empty collections, blank argument validation, event publishing).
 *
 * All collaborators are Mockito mocks; `@Resource`/`@Autowired` caches are reflection-injected, mirroring
 * the project's existing `MsgPublishServicePureTest` precedent. No Spring container or DB is required.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountServicePureTest {

    private val dao = mock(UserAccountDao::class.java)
    private val publishedEvents = mutableListOf<Any>()
    private val recordingPublisher = org.springframework.context.ApplicationEventPublisher { e -> publishedEvents.add(e) }

    private val userOrgHashCache = mock(UserOrgHashCache::class.java)
    private val userAccountHashCache = mock(UserAccountHashCache::class.java)
    private val orgIdsByUserIdCache = mock(OrgIdsByUserIdCache::class.java)

    private fun build(publisher: org.springframework.context.ApplicationEventPublisher = recordingPublisher): UserAccountService {
        val svc = UserAccountService(dao, publisher)
        inject(svc, "userOrgHashCache", userOrgHashCache)
        inject(svc, "userAccountHashCache", userAccountHashCache)
        inject(svc, "orgIdsByUserIdCache", orgIdsByUserIdCache)
        return svc
    }

    private fun inject(target: Any, name: String, value: Any) {
        val f = UserAccountService::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    // Kotlin non-null params reject Mockito's null-returning ArgumentMatchers.any(); these helpers register
    // the matcher (side effect) yet return a real non-null value so the Kotlin caller-side null-check passes.
    private fun anyAccount(): UserAccount =
        ArgumentMatchers.any(UserAccount::class.java) ?: UserAccount { id = "x" }

    @Suppress("UNCHECKED_CAST")
    private fun anyMapArg(): Map<String, *> =
        (ArgumentMatchers.any(Map::class.java) as Map<String, *>?) ?: emptyMap<String, Any?>()

    /** ArgumentCaptor.capture() returns null, which a non-null Kotlin param rejects; coalesce to a real value. */
    private fun captureAccount(captor: org.mockito.ArgumentCaptor<UserAccount>): UserAccount =
        captor.capture() ?: UserAccount { id = "x" }

    /** eq() also returns null; coalesce so a non-null Kotlin String param accepts it. */
    private fun eqStr(value: String): String = eq(value) ?: value

    @AfterTest
    fun clear() = publishedEvents.clear()

    // ---- read-through delegations ----

    @Test
    fun getUserOrgIds_delegatesToCache() {
        whenCalled(orgIdsByUserIdCache.getOrgIds("u1")).thenReturn(listOf("o1", "o2"))
        assertEquals(listOf("o1", "o2"), build().getUserOrgIds("u1"))
    }

    @Test
    fun getUserIds_delegatesToDao() {
        whenCalled(dao.searchActiveUserIdsByTenantId("t1")).thenReturn(listOf("u1", "u2"))
        assertEquals(listOf("u1", "u2"), build().getUserIds("t1"))
    }

    @Test
    fun getUserOrgs_emptyOrgIds_returnsEmptyWithoutHittingOrgCache() {
        whenCalled(orgIdsByUserIdCache.getOrgIds("u1")).thenReturn(emptyList())
        val result = build().getUserOrgs("u1")
        assertTrue(result.isEmpty())
        verify(userOrgHashCache, never()).getOrgsByIds(anyCollection())
    }

    @Test
    fun getUserOrgs_mapsAndDropsMissingEntries_preservingOrgIdOrder() {
        val o1 = mock(UserOrgCacheEntry::class.java)
        val o3 = mock(UserOrgCacheEntry::class.java)
        whenCalled(orgIdsByUserIdCache.getOrgIds("u1")).thenReturn(listOf("o1", "o2", "o3"))
        // o2 is absent from the map -> mapNotNull drops it; order follows orgIds, not the map.
        whenCalled(userOrgHashCache.getOrgsByIds(listOf("o1", "o2", "o3")))
            .thenReturn(mapOf("o3" to o3, "o1" to o1))
        val result = build().getUserOrgs("u1")
        assertEquals(listOf(o1, o3), result)
    }

    @Test
    fun isUserInOrg_trueWhenContained_falseOtherwise() {
        whenCalled(orgIdsByUserIdCache.getOrgIds("u1")).thenReturn(listOf("o1", "o2"))
        val svc = build()
        assertTrue(svc.isUserInOrg("u1", "o2"))
        assertFalse(svc.isUserInOrg("u1", "o9"))
    }

    @Test
    fun getUserByTenantIdAndUsername_resolvesViaSecondaryThenPrimary() {
        val secondary = mock(UserAccountCacheEntry::class.java)
        whenCalled(secondary.id).thenReturn("u1")
        val primary = mock(UserAccountCacheEntry::class.java)
        whenCalled(userAccountHashCache.getUsersByTenantIdAndUsername("t1", "alice")).thenReturn(secondary)
        whenCalled(userAccountHashCache.getUserById("u1")).thenReturn(primary)
        assertEquals(primary, build().getUserByTenantIdAndUsername("t1", "alice"))
    }

    @Test
    fun getUserByTenantIdAndUsername_nullWhenSecondaryMiss() {
        whenCalled(userAccountHashCache.getUsersByTenantIdAndUsername("t1", "ghost")).thenReturn(null)
        assertNull(build().getUserByTenantIdAndUsername("t1", "ghost"))
        verify(userAccountHashCache, never()).getUserById(anyString())
    }

    // ---- updateAndPublish template: success vs failure ----

    @Test
    fun updateActive_success_publishesUpdatedEvent() {
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        assertTrue(build().updateActive("u1", false))
        assertEquals(1, publishedEvents.count { it is UserAccountUpdated && it.id == "u1" })
    }

    @Test
    fun updateActive_daoFalse_returnsFalseAndPublishesNothing() {
        whenCalled(dao.update(anyAccount())).thenReturn(false)
        assertFalse(build().updateActive("u1", false))
        assertTrue(publishedEvents.none { it is UserAccountUpdated })
    }

    @Test
    fun resetPassword_hashesAndResetsErrorCount_andPublishes() {
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        assertTrue(build().resetPassword("u1", "secret"))
        // Verify the entity passed to dao carries a hashed (not plaintext) password and zeroed error count.
        val captor = org.mockito.ArgumentCaptor.forClass(UserAccount::class.java)
        verify(dao).update(captureAccount(captor))
        val entity = captor.value
        assertEquals(0, entity.loginErrorTimes)
        assertTrue(entity.loginPassword.isNotBlank() && entity.loginPassword != "secret")
    }

    @Test
    fun resetSecurityPassword_hashesAndResetsErrorCount() {
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        assertTrue(build().resetSecurityPassword("u1", "secret"))
        val captor = org.mockito.ArgumentCaptor.forClass(UserAccount::class.java)
        verify(dao).update(captureAccount(captor))
        assertEquals(0, captor.value.securityPasswordErrorTimes)
        assertTrue(captor.value.securityPassword != "secret")
    }

    @Test
    fun updateLastLoginInfo_writesIpTimeAndResetsErrors() {
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        val t = java.time.LocalDateTime.of(2026, 1, 1, 8, 0)
        assertTrue(build().updateLastLoginInfo("u1", 123L, t))
        val captor = org.mockito.ArgumentCaptor.forClass(UserAccount::class.java)
        verify(dao).update(captureAccount(captor))
        assertEquals(123L, captor.value.lastLoginIp)
        assertEquals(t, captor.value.lastLoginTime)
        assertEquals(0, captor.value.loginErrorTimes)
    }

    @Test
    fun updateLastLogoutInfo_writesLogoutTime() {
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        val t = java.time.LocalDateTime.of(2026, 1, 1, 18, 0)
        assertTrue(build().updateLastLogoutInfo("u1", t))
        val captor = org.mockito.ArgumentCaptor.forClass(UserAccount::class.java)
        verify(dao).update(captureAccount(captor))
        assertEquals(t, captor.value.lastLogoutTime)
    }

    @Test
    fun resetLoginErrorTimes_setsZero() {
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        assertTrue(build().resetLoginErrorTimes("u1"))
    }

    @Test
    fun resetSecurityPasswordErrorTimes_setsZero() {
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        assertTrue(build().resetSecurityPasswordErrorTimes("u1"))
    }

    // ---- increment*: missing-user short-circuit (returns false) vs existing ----

    @Test
    fun incrementLoginErrorTimes_missingUser_returnsFalse() {
        whenCalled(dao.get("ghost")).thenReturn(null)
        assertFalse(build().incrementLoginErrorTimes("ghost"))
        verify(dao, never()).update(anyAccount())
    }

    @Test
    fun incrementLoginErrorTimes_nullCount_startsFromZero() {
        val existing = mock(UserAccount::class.java)
        whenCalled(existing.loginErrorTimes).thenReturn(null)
        whenCalled(dao.get("u1")).thenReturn(existing)
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        assertTrue(build().incrementLoginErrorTimes("u1"))
        val captor = org.mockito.ArgumentCaptor.forClass(UserAccount::class.java)
        verify(dao).update(captureAccount(captor))
        assertEquals(1, captor.value.loginErrorTimes)
    }

    @Test
    fun incrementLoginErrorTimes_existingCount_increments() {
        val existing = mock(UserAccount::class.java)
        whenCalled(existing.loginErrorTimes).thenReturn(4)
        whenCalled(dao.get("u1")).thenReturn(existing)
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        assertTrue(build().incrementLoginErrorTimes("u1"))
        val captor = org.mockito.ArgumentCaptor.forClass(UserAccount::class.java)
        verify(dao).update(captureAccount(captor))
        assertEquals(5, captor.value.loginErrorTimes)
    }

    @Test
    fun incrementSecurityPasswordErrorTimes_missingUser_returnsFalse() {
        whenCalled(dao.get("ghost")).thenReturn(null)
        assertFalse(build().incrementSecurityPasswordErrorTimes("ghost"))
    }

    @Test
    fun incrementSecurityPasswordErrorTimes_nullCount_startsFromZero() {
        val existing = mock(UserAccount::class.java)
        whenCalled(existing.securityPasswordErrorTimes).thenReturn(null)
        whenCalled(dao.get("u1")).thenReturn(existing)
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        assertTrue(build().incrementSecurityPasswordErrorTimes("u1"))
        val captor = org.mockito.ArgumentCaptor.forClass(UserAccount::class.java)
        verify(dao).update(captureAccount(captor))
        assertEquals(1, captor.value.securityPasswordErrorTimes)
    }

    // ---- insert / update / delete overrides ----

    @Test
    fun insert_publishesInsertedEventWithGeneratedId() {
        val po = UserAccount { id = "new-id" }
        whenCalled(dao.insert(po)).thenReturn("new-id")
        val id = build().insert(po)
        assertEquals("new-id", id)
        assertEquals(1, publishedEvents.count { it is UserAccountInserted && it.id == "new-id" })
    }

    @Test
    fun update_success_publishesUpdatedEvent() {
        val po = UserAccount { id = "u1" }
        whenCalled(dao.update(po)).thenReturn(true)
        assertTrue(build().update(po))
        assertEquals(1, publishedEvents.count { it is UserAccountUpdated && it.id == "u1" })
    }

    @Test
    fun update_failure_publishesNothing() {
        val po = UserAccount { id = "u1" }
        whenCalled(dao.update(po)).thenReturn(false)
        assertFalse(build().update(po))
        assertTrue(publishedEvents.none { it is UserAccountUpdated })
    }

    @Test
    fun deleteById_missingUser_returnsFalseAndPublishesNothing() {
        whenCalled(dao.get("ghost")).thenReturn(null)
        assertFalse(build().deleteById("ghost"))
        verify(dao, never()).deleteById(anyString())
        assertTrue(publishedEvents.isEmpty())
    }

    @Test
    fun deleteById_success_publishesDeletedWithSnapshot() {
        val user = mock(UserAccount::class.java)
        whenCalled(user.tenantId).thenReturn("t1")
        whenCalled(user.username).thenReturn("alice")
        whenCalled(dao.get("u1")).thenReturn(user)
        whenCalled(dao.deleteById("u1")).thenReturn(true)
        assertTrue(build().deleteById("u1"))
        val ev = publishedEvents.filterIsInstance<UserAccountDeleted>().single()
        assertEquals("u1", ev.id)
        assertEquals("t1", ev.tenantId)
        assertEquals("alice", ev.username)
    }

    @Test
    fun deleteById_daoReturnsFalse_publishesNothing() {
        val user = mock(UserAccount::class.java)
        whenCalled(user.tenantId).thenReturn("t1")
        whenCalled(user.username).thenReturn("alice")
        whenCalled(dao.get("u1")).thenReturn(user)
        whenCalled(dao.deleteById("u1")).thenReturn(false)
        assertFalse(build().deleteById("u1"))
        assertTrue(publishedEvents.none { it is UserAccountDeleted })
    }

    @Test
    fun batchDelete_empty_returnsZeroAndPublishesNothing() {
        // empty -> snapshots = emptyList; super.batchDelete(empty) -> dao.batchDelete(empty)
        whenCalled(dao.batchDelete(emptyList())).thenReturn(0)
        assertEquals(0, build().batchDelete(emptyList()))
        assertTrue(publishedEvents.none { it is UserAccountBatchDeleted })
    }

    @Test
    fun batchDelete_nonEmpty_snapshotsThenPublishesBatchDeleted() {
        val u1 = mock(UserAccount::class.java)
        whenCalled(u1.id).thenReturn("u1"); whenCalled(u1.tenantId).thenReturn("t1"); whenCalled(u1.username).thenReturn("a")
        val u2 = mock(UserAccount::class.java)
        whenCalled(u2.id).thenReturn("u2"); whenCalled(u2.tenantId).thenReturn("t1"); whenCalled(u2.username).thenReturn("b")
        whenCalled(dao.getByIds(listOf("u1", "u2"))).thenReturn(listOf(u1, u2))
        whenCalled(dao.batchDelete(listOf("u1", "u2"))).thenReturn(2)
        val count = build().batchDelete(listOf("u1", "u2"))
        assertEquals(2, count)
        val ev = publishedEvents.filterIsInstance<UserAccountBatchDeleted>().single()
        assertEquals(listOf("u1", "u2"), ev.ids.toList())
    }

    // ---- auth key ----

    @Test
    fun resetAuthKey_daoUpdateFails_returnsNull() {
        whenCalled(dao.update(anyAccount())).thenReturn(false)
        assertNull(build().resetAuthKey("u1", "alice", "kudos"))
        assertTrue(publishedEvents.isEmpty())
    }

    @Test
    fun resetAuthKey_success_returnsSetupAndPublishes() {
        whenCalled(dao.update(anyAccount())).thenReturn(true)
        val setup = build().resetAuthKey("u1", "alice", "kudos")!!
        assertTrue(setup.secret.isNotBlank())
        assertTrue(setup.otpauthUrl.startsWith("otpauth://totp/"))
        assertTrue(setup.otpauthUrl.contains("issuer=kudos"))
        assertTrue(setup.otpauthUrl.contains("secret=${setup.secret}"))
        assertEquals(1, publishedEvents.count { it is UserAccountUpdated })
    }

    @Test
    fun cleanAuthKey_success_publishes() {
        whenCalled(dao.updateProperties(eqStr("u1"), anyMapArg())).thenReturn(true)
        assertTrue(build().cleanAuthKey("u1"))
        assertEquals(1, publishedEvents.count { it is UserAccountUpdated })
    }

    @Test
    fun cleanAuthKey_failure_returnsFalseNoEvent() {
        whenCalled(dao.updateProperties(eqStr("u1"), anyMapArg())).thenReturn(false)
        assertFalse(build().cleanAuthKey("u1"))
        assertTrue(publishedEvents.isEmpty())
    }

    @Test
    fun verifyAuthCode_nullKey_returnsFalse() {
        val user = mock(UserAccount::class.java)
        whenCalled(user.authenticationKey).thenReturn(null)
        whenCalled(dao.get("u1")).thenReturn(user)
        assertFalse(build().verifyAuthCode("u1", 123456L))
    }

    @Test
    fun verifyAuthCode_missingUser_returnsFalse() {
        whenCalled(dao.get("ghost")).thenReturn(null)
        assertFalse(build().verifyAuthCode("ghost", 123456L))
    }

    // ---- freeze / unfreeze ----

    @Test
    fun freezeAccount_blankType_throwsIllegalArgument() {
        val ex = assertFailsWith<IllegalArgumentException> {
            build().freezeAccount("u1", "  ", null, null, null, null)
        }
        assertTrue(ex.message!!.contains("freezeType"))
        verify(dao, never()).updateProperties(anyString(), anyMapArg())
    }

    @Test
    fun freezeAccount_success_publishes() {
        whenCalled(dao.updateProperties(eqStr("u1"), anyMapArg())).thenReturn(true)
        assertTrue(build().freezeAccount("u1", "manual", "t", "c", null, null))
        assertEquals(1, publishedEvents.count { it is UserAccountUpdated })
    }

    @Test
    fun freezeAccount_failure_noEvent() {
        whenCalled(dao.updateProperties(eqStr("u1"), anyMapArg())).thenReturn(false)
        assertFalse(build().freezeAccount("u1", "manual", null, null, null, null))
        assertTrue(publishedEvents.isEmpty())
    }

    @Test
    fun unfreezeAccount_success_publishes() {
        whenCalled(dao.updateProperties(eqStr("u1"), anyMapArg())).thenReturn(true)
        assertTrue(build().unfreezeAccount("u1"))
        assertEquals(1, publishedEvents.count { it is UserAccountUpdated })
    }

    @Test
    fun unfreezeAccount_failure_noEvent() {
        whenCalled(dao.updateProperties(eqStr("u1"), anyMapArg())).thenReturn(false)
        assertFalse(build().unfreezeAccount("u1"))
        assertTrue(publishedEvents.isEmpty())
    }

    // cleanExpiredFreezes is covered by the integration test (UserAccountServiceTest); its dao.searchAs
    // call is an inline-reified function that Mockito cannot stub, so it is intentionally not duplicated here.
}
