package io.kudos.ms.user.core.org.service

import io.kudos.ms.user.core.org.spi.IOrganizationMemberPolicy
import io.kudos.ms.user.core.security.PLATFORM_ADMIN
import io.kudos.ms.user.core.security.PLATFORM_TENANT
import io.kudos.ms.user.core.security.accessGuard
import io.kudos.ms.user.core.security.directoryGate
import io.kudos.ms.user.core.security.organizationMode
import io.kudos.ms.user.core.security.signIn
import io.kudos.ms.user.core.security.signOut
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure tests of [io.kudos.ms.user.core.org.service.impl.OrganizationDirectoryGate]: the mode switch,
 * the "trusted only" fallback without a member policy, policy delegation, and the legacy-tenant rule.
 */
internal class OrganizationDirectoryGateTest {

    private val policy = mock(IOrganizationMemberPolicy::class.java)

    @AfterTest
    fun clear() = signOut()

    // ---- mode off ----

    @Test
    fun modeOffRefusesEveryOrganizationWriteBeforeConsultingThePolicy() {
        val gate = directoryGate(organizationMode(false), accessGuard(), policy)
        assertFalse(gate.organizationModeEnabled)
        val writes: List<() -> Unit> = listOf(
            { gate.assertCanManageDirectory("org-1") },
            { gate.assertCanManageMember("org-1", null) },
            { gate.assertCanEndMembership("org-1", "m1") },
            { gate.assertPlatform() },
        )
        writes.forEach { write ->
            val e = assertFailsWith<IllegalStateException> { write() }
            assertEquals("ORGANIZATION_MODE_DISABLED", e.message)
        }
        verifyNoInteractions(policy)
    }

    @Test
    fun modeOffNeverLetsAnOrganizationAccountSignIn() {
        whenCalled(policy.canSignIn("tenant-a", "m1")).thenReturn(true)
        val gate = directoryGate(organizationMode(false), accessGuard(), policy)
        assertFalse(gate.canSignIn("tenant-a", "m1"))
        verifyNoInteractions(policy)
    }

    @Test
    fun modeOffAllowsLegacyRowsInAnyTenant() {
        val gate = directoryGate(organizationMode(false), accessGuard())
        gate.assertLegacyTenantAllowed("tenant-a")
        gate.assertLegacyTenantAllowed(PLATFORM_TENANT)
        gate.assertLegacyTenantAllowed(null)
    }

    // ---- mode on, no policy: trusted only ----

    @Test
    fun withoutPolicyTrustedInternalWorkMayWrite() {
        val gate = directoryGate(organizationMode(true), accessGuard())
        assertTrue(gate.organizationModeEnabled)
        gate.assertCanManageDirectory("org-1")
        gate.assertCanManageMember("org-1", "m1")
        gate.assertCanEndMembership("org-1", "m1")
        gate.assertPlatform()
    }

    @Test
    fun withoutPolicyPlatformAdministratorMayWrite() {
        val gate = directoryGate(organizationMode(true), accessGuard())
        signIn(PLATFORM_ADMIN, PLATFORM_TENANT)
        gate.assertCanManageDirectory("org-1")
        gate.assertCanEndMembership("org-1", "m1")
        gate.assertPlatform()
    }

    @Test
    fun withoutPolicyOrganizationMemberIsRefusedEvenInItsOwnOrganization() {
        val gate = directoryGate(organizationMode(true), accessGuard())
        signIn("m1", "tenant-a", organizationId = "org-1")
        val writes: List<() -> Unit> = listOf(
            { gate.assertCanManageDirectory("org-1") },
            { gate.assertCanManageMember("org-1", null) },
            { gate.assertCanEndMembership("org-1", "m2") },
            { gate.assertPlatform() },
        )
        writes.forEach { write ->
            val e = assertFailsWith<IllegalArgumentException> { write() }
            assertEquals("ORGANIZATION_MANAGEMENT_FORBIDDEN", e.message)
        }
    }

    @Test
    fun withoutPolicyNobodySignsIn() {
        val gate = directoryGate(organizationMode(true), accessGuard())
        assertFalse(gate.canSignIn("tenant-a", "m1"))
    }

    // ---- mode on, with policy ----

    @Test
    fun policyDecidesForMembersOfTheOrganization() {
        val gate = directoryGate(organizationMode(true), accessGuard(), policy)
        signIn("m1", "tenant-a", organizationId = "org-1")
        gate.assertCanManageDirectory("org-1")
        gate.assertCanManageMember("org-1", "m2")
        gate.assertCanEndMembership("org-1", "m3")
        verify(policy).assertCanManageDirectory("org-1")
        verify(policy).assertCanManageMember("org-1", "m2")
        verify(policy).assertCanEndMembership("org-1", "m3")
    }

    @Test
    fun policyRefusalPropagates() {
        doThrow(IllegalArgumentException("LAST_ORGANIZATION_ADMIN")).`when`(policy).assertCanEndMembership("org-1", "m2")
        val gate = directoryGate(organizationMode(true), accessGuard(), policy)
        signIn("m1", "tenant-a", organizationId = "org-1")
        val e = assertFailsWith<IllegalArgumentException> { gate.assertCanEndMembership("org-1", "m2") }
        assertEquals("LAST_ORGANIZATION_ADMIN", e.message)
    }

    @Test
    fun otherOrganizationIsRefusedBeforeThePolicyIsAsked() {
        val gate = directoryGate(organizationMode(true), accessGuard(), policy)
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { gate.assertCanManageDirectory("org-2") }
        assertFailsWith<IllegalArgumentException> { gate.assertCanManageMember("org-2", null) }
        assertFailsWith<IllegalArgumentException> { gate.assertCanEndMembership("org-2", "x") }
        verifyNoInteractions(policy)
    }

    @Test
    fun legacyPrincipalIsRefusedBeforeThePolicyIsAsked() {
        val gate = directoryGate(organizationMode(true), accessGuard(), policy)
        signIn("u1", "tenant-a")
        assertFailsWith<IllegalArgumentException> { gate.assertCanManageDirectory("org-1") }
        verifyNoInteractions(policy)
    }

    @Test
    fun policyIsAlsoAskedForTrustedWork() {
        // With a policy deployed, the policy decides; the gate does not short-circuit internal work.
        val gate = directoryGate(organizationMode(true), accessGuard(), policy)
        gate.assertCanManageDirectory("org-1")
        verify(policy).assertCanManageDirectory("org-1")
    }

    @Test
    fun platformOperationsIgnoreThePolicy() {
        val gate = directoryGate(organizationMode(true), accessGuard(), policy)
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { gate.assertPlatform() }
        signIn(PLATFORM_ADMIN, PLATFORM_TENANT)
        gate.assertPlatform()
        verifyNoInteractions(policy)
    }

    @Test
    fun canSignInDelegatesToThePolicy() {
        whenCalled(policy.canSignIn("tenant-a", "m1")).thenReturn(true)
        whenCalled(policy.canSignIn("tenant-b", "m1")).thenReturn(false)
        val gate = directoryGate(organizationMode(true), accessGuard(), policy)
        assertTrue(gate.canSignIn("tenant-a", "m1"))
        assertFalse(gate.canSignIn("tenant-b", "m1"))
    }

    // ---- legacy tenants in organization mode ----

    @Test
    fun modeOnAllowsLegacyRowsOnlyInPlatformTenants() {
        val gate = directoryGate(organizationMode(true), accessGuard())
        gate.assertLegacyTenantAllowed(PLATFORM_TENANT)
        listOf("tenant-a", "", null).forEach { tenant ->
            val e = assertFailsWith<IllegalArgumentException> { gate.assertLegacyTenantAllowed(tenant) }
            assertEquals("ORGANIZATION_REQUIRED", e.message)
        }
    }

    @Test
    fun severalPlatformTenantsAreAllRecognised() {
        val gate = directoryGate(organizationMode(true, setOf("p1", "p2")), accessGuard())
        gate.assertLegacyTenantAllowed("p1")
        gate.assertLegacyTenantAllowed("p2")
        assertFailsWith<IllegalArgumentException> { gate.assertLegacyTenantAllowed(PLATFORM_TENANT) }
    }
}
