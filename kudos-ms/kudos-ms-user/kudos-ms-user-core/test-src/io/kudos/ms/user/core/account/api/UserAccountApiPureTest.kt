package io.kudos.ms.user.core.account.api

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [UserAccountApi] — verifies that every API method delegates to the right
 * collaborator (cache vs service) and forwards the result unchanged, including the null / id-extraction
 * paths. Collaborators are Mockito mocks, reflection-injected into the `@Autowired` fields.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountApiPureTest {

    private val cache = mock(UserAccountHashCache::class.java)
    private val service = mock(IUserAccountService::class.java)

    private fun build(): UserAccountApi {
        val api = UserAccountApi()
        inject(api, "userAccountHashCache", cache)
        inject(api, "userAccountService", service)
        return api
    }

    private fun inject(target: Any, name: String, value: Any) {
        val f = UserAccountApi::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    @Test
    fun getUserById_delegatesToCache() {
        val entry = mock(UserAccountCacheEntry::class.java)
        whenCalled(cache.getUserById("u1")).thenReturn(entry)
        assertEquals(entry, build().getUserById("u1"))
    }

    @Test
    fun getUsersByIds_delegatesToCache() {
        val entry = mock(UserAccountCacheEntry::class.java)
        whenCalled(cache.getUsersByIds(listOf("u1"))).thenReturn(mapOf("u1" to entry))
        assertEquals(mapOf("u1" to entry), build().getUsersByIds(listOf("u1")))
    }

    @Test
    fun getUserId_returnsEntryIdWhenFound() {
        val entry = mock(UserAccountCacheEntry::class.java)
        whenCalled(entry.id).thenReturn("u1")
        whenCalled(cache.getUsersByTenantIdAndUsername("t1", "alice")).thenReturn(entry)
        assertEquals("u1", build().getUserId("t1", "alice"))
    }

    @Test
    fun getUserId_nullWhenNotFound() {
        whenCalled(cache.getUsersByTenantIdAndUsername("t1", "ghost")).thenReturn(null)
        assertNull(build().getUserId("t1", "ghost"))
    }

    @Test
    fun isUserInOrg_delegatesToService() {
        whenCalled(service.isUserInOrg("u1", "o1")).thenReturn(true)
        whenCalled(service.isUserInOrg("u1", "o9")).thenReturn(false)
        val api = build()
        assertTrue(api.isUserInOrg("u1", "o1"))
        assertFalse(api.isUserInOrg("u1", "o9"))
    }

    @Test
    fun getUserOrgs_delegatesToService() {
        val org = mock(UserOrgCacheEntry::class.java)
        whenCalled(service.getUserOrgs("u1")).thenReturn(listOf(org))
        assertEquals(listOf(org), build().getUserOrgs("u1"))
    }

    @Test
    fun getUserIds_delegatesToService() {
        whenCalled(service.getUserIds("t1")).thenReturn(listOf("u1", "u2"))
        val api = build()
        assertEquals(listOf("u1", "u2"), api.getUserIds("t1"))
        verify(service).getUserIds("t1")
    }
}
