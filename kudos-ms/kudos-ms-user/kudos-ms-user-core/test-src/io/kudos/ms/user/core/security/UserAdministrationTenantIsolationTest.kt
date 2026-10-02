package io.kudos.ms.user.core.security

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.account.vo.request.UserAccountQuery
import io.kudos.ms.user.common.account.vo.response.UserAccountDetail
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.common.security.IPlatformAdministratorPolicy
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.security.IPasswordPolicy
import io.kudos.ms.user.core.account.service.impl.UserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.*

internal class UserAdministrationTenantIsolationTest {
    private val dao = mock(UserAccountDao::class.java)
    private val encoder = mock(PasswordEncoder::class.java)
    private val publisher = mock(ApplicationEventPublisher::class.java)
    private val service = UserAccountService(dao, publisher, mock(IPasswordPolicy::class.java), emptyList(), encoder)

    @BeforeTest
    fun login() = bindPrincipal("operator", "tenant-a")

    @AfterTest
    fun clear() = KudosContextHolder.clear()

    @Test
    fun crossTenantPasswordTotpFreezeAndCrudAreRejectedBeforeAnyWrite() {
        `when`(dao.get("victim")).thenReturn(account("victim", "tenant-b"))
        val operations: List<() -> Any?> = listOf(
            { service.resetPassword("victim", "changed-password") },
            { service.resetSecurityPassword("victim", "changed-password") },
            { service.cleanAuthKey("victim") },
            { service.resetAuthKey("victim", "victim", "kudos") },
            { service.updateActive("victim", true) },
            { service.unfreezeAccount("victim") },
            { service.get("victim", UserAccountDetail::class) },
            { service.update(UserAccount { id = "victim"; tenantId = "tenant-a" }) },
            { service.deleteById("victim") },
        )
        operations.forEach { action -> assertFailsWith<IllegalArgumentException> { action() } }
        verifyNoInteractions(encoder, publisher)
        assertTrue(mockingDetails(dao).invocations.all { it.method.name == "get" })
    }

    @Test
    fun mixedTenantBatchIsRejectedBeforeDeletingAnyRow() {
        val ids = listOf("own", "victim")
        `when`(dao.getByIds(ids)).thenReturn(listOf(account("own", "tenant-a"), account("victim", "tenant-b")))
        assertFailsWith<IllegalArgumentException> { service.batchDelete(ids) }
        assertEquals(listOf("getByIds"), mockingDetails(dao).invocations.map { it.method.name })
    }

    @Test
    fun omittedListTenantIsFilledFromAuthenticatedPrincipal() {
        val request = UserAccountQuery().apply { pageNo = 3; pageSize = 17 }
        val captor = ArgumentCaptor.forClass(ListSearchPayload::class.java)
        `when`(dao.search(ArgumentMatchers.any(ListSearchPayload::class.java))).thenReturn(emptyList<Any>())
        service.pagingSearch(request)
        verify(dao).search(captor.capture())
        val scoped = captor.value as UserAccountQuery
        assertEquals("tenant-a", scoped.tenantId)
        assertEquals(3, scoped.pageNo)
        assertEquals(17, scoped.pageSize)
        assertNull(request.tenantId)
    }

    @Test
    fun explicitForeignListTenantAndCreateAreRejected() {
        assertFailsWith<IllegalArgumentException> { service.pagingSearch(UserAccountQuery(tenantId = "tenant-b")) }
        assertFailsWith<IllegalArgumentException> { service.insert(account("new", "tenant-b")) }
        verifyNoInteractions(dao, encoder, publisher)
    }

    @Test
    fun sameTenantAndVerifiedPlatformAdministratorCanRead() {
        val own = account("own", "tenant-a")
        val foreign = account("foreign", "tenant-b")
        `when`(dao.get("own")).thenReturn(own)
        `when`(dao.get("foreign")).thenReturn(foreign)
        assertSame(own, service.get("own"))
        val guard = UserTenantAccessGuard()
        inject(guard, "platformAdministratorPolicies", listOf(IPlatformAdministratorPolicy { it == "platform-operator" }))
        inject(service, "tenantAccess", guard)
        bindPrincipal("platform-operator", "platform")
        assertSame(foreign, service.get("foreign"))
    }

    private fun account(id: String, tenant: String) = UserAccount {
        this.id = id; tenantId = tenant; username = id; loginPassword = ""; supervisorId = ""
    }
}

internal fun bindPrincipal(id: String, tenant: String) {
    KudosContextHolder.set(KudosContext().apply { user = SessionUserPrincipal(id, tenant, id) })
}

internal fun inject(target: Any, name: String, value: Any) {
    var type: Class<*>? = target.javaClass
    while (type != null) {
        val field = type.declaredFields.firstOrNull { it.name == name }
        if (field != null) { field.isAccessible = true; field.set(target, value); return }
        type = type.superclass
    }
    error("No field $name")
}
