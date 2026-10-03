package io.kudos.ms.user.core.security

import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import io.kudos.ms.user.core.security.UserTenantAccessGuard.Companion.ownerKey
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure tests of the owner boundary of [UserTenantAccessGuard] for organization mode: scopes, the
 * organization/tenant split of row checks, work-in-tenant checks and account-serves-tenant.
 */
internal class UserTenantAccessGuardOrganizationTest {

    private val ownership = mock(IOrganizationOwnershipService::class.java).also {
        whenCalled(it.organizationIdForTenant("tenant-a")).thenReturn("org-1")
        whenCalled(it.organizationIdForTenant("tenant-b")).thenReturn("org-1")
        whenCalled(it.organizationIdForTenant("tenant-x")).thenReturn("org-2")
        whenCalled(it.organizationIdForTenant("unassociated")).thenReturn(null)
    }
    private val guard = accessGuard(ownership)

    @AfterTest
    fun clear() = signOut()

    // ---- scopes ----

    @Test
    fun noPrincipalIsUnrestricted() {
        assertEquals(UserAccessScope.Unrestricted, guard.restrictedScope())
        assertNull(guard.restrictedTenantId())
        assertFalse(guard.hasPrincipal())
        guard.assertCanAccess("any-tenant")
        guard.assertCanAccessOrganization("any-org")
        guard.assertCanAccessOwner("", "any-org")
    }

    @Test
    fun legacyPrincipalIsConfinedToItsTenant() {
        signIn("u1", "tenant-a")
        assertEquals(UserAccessScope.Tenant("tenant-a"), guard.restrictedScope())
        assertEquals("tenant-a", guard.restrictedTenantId())
        guard.assertCanAccess("tenant-a")
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccess("tenant-b") }
        // Legacy principals never reach organization-owned rows.
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccessOrganization("org-1") }
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccessOwner("", "org-1") }
        guard.assertCanAccessOwner("tenant-a", null)
    }

    @Test
    fun blankOrganizationIdIsTreatedAsLegacyPrincipal() {
        signIn("u1", "tenant-a", organizationId = " ")
        assertEquals(UserAccessScope.Tenant("tenant-a"), guard.restrictedScope())
    }

    @Test
    fun organizationPrincipalIsConfinedToItsOrganization() {
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertEquals(UserAccessScope.Organization("org-1"), guard.restrictedScope())
        guard.assertCanAccessOrganization("org-1")
        guard.assertCanAccessOwner("", "org-1")
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccessOrganization("org-2") }
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccessOwner("", "org-2") }
    }

    @Test
    fun organizationPrincipalIsRefusedOnTenantOwnedRowsEvenOfItsLoginTenant() {
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { guard.restrictedTenantId() }
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccess("tenant-a") }
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccess("tenant-b") }
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccessOwner("tenant-a", null) }
        assertFailsWith<IllegalArgumentException> { guard.queryTenantId(null) }
    }

    @Test
    fun organizationPrincipalWithoutTenantStillHasOrganizationScope() {
        // ORGANIZATION-scope sessions carry an empty tenant id.
        signIn("m1", "", organizationId = "org-1")
        assertEquals(UserAccessScope.Organization("org-1"), guard.restrictedScope())
    }

    @Test
    fun platformAdministratorIsUnrestrictedEvenWithOrganization() {
        signIn(PLATFORM_ADMIN, PLATFORM_TENANT, organizationId = "org-1")
        assertEquals(UserAccessScope.Unrestricted, guard.restrictedScope())
        assertNull(guard.restrictedTenantId())
        guard.assertCanAccess("tenant-x")
        guard.assertCanAccessOrganization("org-2")
    }

    @Test
    fun blankTargetsAreRejected() {
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccessOrganization("") }
        signIn("u1", "tenant-a")
        assertFailsWith<IllegalArgumentException> { guard.assertCanAccess("") }
    }

    // ---- assertCanWorkInTenant ----

    @Test
    fun organizationPrincipalWorksOnlyInTenantsOfItsOrganization() {
        signIn("m1", "tenant-a", organizationId = "org-1")
        guard.assertCanWorkInTenant("tenant-a")
        guard.assertCanWorkInTenant("tenant-b")
        assertFailsWith<IllegalArgumentException> { guard.assertCanWorkInTenant("tenant-x") }
        assertFailsWith<IllegalArgumentException> { guard.assertCanWorkInTenant("unassociated") }
        assertFailsWith<IllegalArgumentException> { guard.assertCanWorkInTenant(PLATFORM_TENANT) }
    }

    @Test
    fun organizationPrincipalCannotWorkAnywhereWithoutOwnershipFacts() {
        val bare = accessGuard()
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { bare.assertCanWorkInTenant("tenant-a") }
    }

    @Test
    fun legacyPrincipalWorksOnlyInItsTenant() {
        signIn("u1", "tenant-a")
        guard.assertCanWorkInTenant("tenant-a")
        assertFailsWith<IllegalArgumentException> { guard.assertCanWorkInTenant("tenant-b") }
    }

    @Test
    fun trustedWorkMayWorkInAnyTenant() {
        guard.assertCanWorkInTenant("tenant-x")
        guard.assertCanWorkInTenant("unassociated")
    }

    // ---- accountServesTenant ----

    @Test
    fun legacyAccountServesOnlyItsOwnTenant() {
        assertTrue(guard.accountServesTenant("tenant-a", null, "tenant-a"))
        assertTrue(guard.accountServesTenant("tenant-a", "", "tenant-a"))
        assertFalse(guard.accountServesTenant("tenant-a", null, "tenant-b"))
    }

    @Test
    fun organizationAccountServesEveryTenantOfItsOrganizationOnly() {
        assertTrue(guard.accountServesTenant("", "org-1", "tenant-a"))
        assertTrue(guard.accountServesTenant("", "org-1", "tenant-b"))
        assertFalse(guard.accountServesTenant("", "org-1", "tenant-x"))
        assertFalse(guard.accountServesTenant("", "org-1", "unassociated"))
        // Without ownership facts nothing is served.
        assertFalse(accessGuard().accountServesTenant("", "org-1", "tenant-a"))
    }

    // ---- ownerKey ----

    @Test
    fun ownerKeySeparatesOrganizationsFromTenants() {
        assertEquals("TENANT:t1", ownerKey("t1", null))
        assertEquals("TENANT:t1", ownerKey("t1", ""))
        assertEquals("ORG:o1", ownerKey("", "o1"))
        assertEquals("ORG:o1", ownerKey("t1", "o1"))
        assertEquals("TENANT:", ownerKey(null, null))
        // An organization id equal to a tenant id still yields a different owner.
        assertFalse(ownerKey("same", null) == ownerKey("", "same"))
    }
}
