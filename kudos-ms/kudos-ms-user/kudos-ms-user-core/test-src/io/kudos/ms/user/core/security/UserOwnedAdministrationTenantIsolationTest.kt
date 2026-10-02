package io.kudos.ms.user.core.security

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.contact.vo.request.UserContactWayQuery
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.contact.dao.UserContactWayDao
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import io.kudos.ms.user.core.contact.service.impl.UserContactWayService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import kotlin.test.*

internal class UserOwnedAdministrationTenantIsolationTest {
    private val dao = mock(UserContactWayDao::class.java)
    private val accountDao = mock(UserAccountDao::class.java)
    private val service = UserContactWayService(dao).also { inject(it, "ownerAccountDao", accountDao) }

    @BeforeTest
    fun login() {
        bindPrincipal("operator", "tenant-a")
        `when`(accountDao.get("own")).thenReturn(account("own", "tenant-a"))
        `when`(accountDao.get("foreign")).thenReturn(account("foreign", "tenant-b"))
    }

    @AfterTest
    fun clear() = KudosContextHolder.clear()

    @Test
    fun childRecordsWithoutTenantColumnAuthorizeTheirStoredOwner() {
        val row = UserContactWay { id = "contact"; userId = "foreign" }
        `when`(dao.get("contact")).thenReturn(row)
        assertFailsWith<IllegalArgumentException> { service.get("contact") }
        assertFailsWith<IllegalArgumentException> { service.deleteById("contact") }
        assertFailsWith<IllegalArgumentException> { service.update(UserContactWay { id = "contact"; userId = "own" }) }
        assertFailsWith<IllegalArgumentException> { service.insert(row) }
        assertTrue(mockingDetails(dao).invocations.all { it.method.name == "get" })
    }

    @Test
    fun listRestrictionIsAppliedBeforeDaoPaginationAndCount() {
        val request = UserContactWayQuery(userId = "own", contactWayDictCode = "EMAIL").apply { pageNo = 2; pageSize = 10 }
        `when`(dao.search(ArgumentMatchers.any(ListSearchPayload::class.java))).thenReturn(emptyList<Any>())
        service.pagingSearch(request)
        val captor = ArgumentCaptor.forClass(ListSearchPayload::class.java)
        verify(dao).search(captor.capture())
        val payload = captor.value
        assertEquals(2, payload.pageNo)
        assertEquals(10, payload.pageSize)
        assertEquals(listOf("own"), payload.getCriterions()!!.last { it.property == "userId" }.value)
        assertTrue(payload.getCriterions()!!.any { it.property == "contactWayDictCode" && it.value == "EMAIL" })
    }

    @Test
    fun foreignOwnerFilterAndChangingOwnershipAreRejected() {
        assertFailsWith<IllegalArgumentException> { service.pagingSearch(UserContactWayQuery(userId = "foreign")) }
        `when`(dao.get("contact")).thenReturn(UserContactWay { id = "contact"; userId = "own" })
        assertFailsWith<IllegalArgumentException> { service.update(UserContactWay { id = "contact"; userId = "foreign" }) }
        assertTrue(mockingDetails(dao).invocations.all { it.method.name == "get" })
    }

    private fun account(id: String, tenant: String) = UserAccount { this.id = id; tenantId = tenant }
}
