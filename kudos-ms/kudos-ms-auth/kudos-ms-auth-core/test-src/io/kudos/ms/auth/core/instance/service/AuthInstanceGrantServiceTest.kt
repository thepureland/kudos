package io.kudos.ms.auth.core.instance.service

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.instance.service.iservice.IAuthInstanceGrantService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * junit test for instance-level sharing and how it merges into the decision algebra.
 *
 * Test data source: `AuthInstanceGrantServiceTest.sql`.
 *
 * Two things are being pinned: that a share genuinely adds reach the role model cannot express, and
 * that it adds *only* that — in particular that it can never overturn a DENY, or sharing would
 * become the documented way around a revocation.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthInstanceGrantServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthInstanceGrantService

    @Resource
    private lateinit var authzDecisionApi: IAuthzDecisionApi

    private val guest = "ins10000-0000-0000-0000-000000000002"
    private val blocked = "ins10000-0000-0000-0000-000000000003"
    private val owner = "ins10000-0000-0000-0000-000000000001"
    private val tenant = "tenant-ins-1-Cq3"

    private fun decide(userId: String, code: String, type: String? = null, instance: String? = null) =
        authzDecisionApi.decide(
            AuthzRequest(SubjectRef.ofUser(userId), code, resourceType = type, instanceId = instance),
        )

    @Test
    fun aShareReachesSomebodyWithNoRoleAtAll() {
        assertFalse(decide(guest, "doc:read", "doc", "doc-1").permitted)

        service.share(guest, tenant, "doc", "doc-1", "doc:read", grantedBy = owner)

        val decision = decide(guest, "doc:read", "doc", "doc-1")
        assertTrue(decision.permitted)
        assertEquals(AuthzDecision.Reason.ALLOWED_BY_INSTANCE_GRANT, decision.reason)
        // And only that instance: a share is not a role.
        assertFalse(decide(guest, "doc:read", "doc", "doc-2").permitted)
        assertFalse(decide(guest, "doc:read").permitted, "and it says nothing about documents in general")
    }

    @Test
    fun aShareCanNarrowTheActionOrCoverEverything() {
        service.share(guest, tenant, "doc", "doc-3", "doc:read")
        assertTrue(decide(guest, "doc:read", "doc", "doc-3").permitted)
        assertFalse(decide(guest, "doc:update", "doc", "doc-3").permitted, "read is not write")

        service.share(guest, tenant, "doc", "doc-4", "*")
        assertTrue(decide(guest, "doc:update", "doc", "doc-4").permitted, "a wildcard share covers the instance")
    }

    @Test
    fun anInstanceDenyBeatsARoleWideAllow() {
        // The case roles cannot express: everyone in this role may read documents, except this one
        // person on this one document.
        assertTrue(decide(blocked, "doc:read", "doc", "doc-5").permitted)

        service.share(blocked, tenant, "doc", "doc-5", "doc:read", effect = "DENY")

        val decision = decide(blocked, "doc:read", "doc", "doc-5")
        assertFalse(decision.permitted)
        assertEquals(AuthzDecision.Reason.DENIED_BY_RULE, decision.reason)
        assertTrue(decide(blocked, "doc:read", "doc", "doc-6").permitted, "other documents are unaffected")
    }

    @Test
    fun aWindowedShareIsIgnoredOutsideItsWindow() {
        val now = LocalDateTime.now()
        service.share(guest, tenant, "doc", "doc-7", "doc:read", startTime = now.plusDays(1), endTime = now.plusDays(2))
        assertFalse(decide(guest, "doc:read", "doc", "doc-7").permitted, "not yet effective")

        service.share(guest, tenant, "doc", "doc-8", "doc:read", startTime = now.minusDays(2), endTime = now.minusDays(1))
        assertFalse(decide(guest, "doc:read", "doc", "doc-8").permitted, "already expired")
    }

    @Test
    fun sharingTwiceUpdatesRatherThanDuplicates() {
        val first = service.share(guest, tenant, "doc", "doc-9", "doc:read")
        val second = service.share(guest, tenant, "doc", "doc-9", "doc:read", effect = "DENY")
        assertEquals(first, second, "the same tuple must adjust the share, not leave two of them")
        assertEquals(1, service.listShares("doc", "doc-9").size)
        assertEquals("DENY", service.listShares("doc", "doc-9").single().effect)
    }

    @Test
    fun unsharingIsSoftAndTakesEffectImmediately() {
        val id = service.share(guest, tenant, "doc", "doc-10", "doc:read")
        assertTrue(decide(guest, "doc:read", "doc", "doc-10").permitted)

        assertTrue(service.unshare(id, "no longer needed"))
        assertFalse(decide(guest, "doc:read", "doc", "doc-10").permitted)
        assertTrue(service.listShares("doc", "doc-10").isEmpty())
        // The row survives, so the un-share stays answerable afterwards.
        assertEquals("no longer needed", service.get(id)!!.revokeReason)
        assertFalse(service.unshare(id), "withdrawing twice is a no-op")
    }

    @Test
    fun resharingSomethingWithdrawnRevivesIt() {
        val id = service.share(guest, tenant, "doc", "doc-11", "doc:read")
        service.unshare(id, "oops")
        service.share(guest, tenant, "doc", "doc-11", "doc:read")
        assertTrue(
            decide(guest, "doc:read", "doc", "doc-11").permitted,
            "a dead row must not shadow the new intent",
        )
    }

    @Test
    fun listSharesAnswersWhoThisIsSharedWith() {
        service.share(guest, tenant, "doc", "doc-12", "doc:read")
        service.share(blocked, tenant, "doc", "doc-12", "doc:update")
        assertEquals(setOf(guest, blocked), service.listShares("doc", "doc-12").map { it.principalId }.toSet())
    }

    @Test
    fun aShareMustNameItsRecipientInstanceAndAction() {
        assertFailsWith<IllegalArgumentException> { service.share("", tenant, "doc", "doc-13", "doc:read") }
        assertFailsWith<IllegalArgumentException> { service.share(guest, tenant, "doc", "", "doc:read") }
        assertFailsWith<IllegalArgumentException> { service.share(guest, tenant, "doc", "doc-13", "") }
        assertFailsWith<IllegalArgumentException> {
            val now = LocalDateTime.now()
            service.share(guest, tenant, "doc", "doc-13", "doc:read", startTime = now.plusDays(2), endTime = now)
        }
    }

    @Test
    fun aQuestionThatNamesNoInstanceNeverConsultsShares() {
        // The cost note in AuthzDecisionApi depends on this: the extra database read only happens
        // when the caller actually asks about a particular thing.
        service.share(guest, tenant, "doc", "doc-14", "doc:read")
        assertFalse(decide(guest, "doc:read").permitted)
    }

    /** The sharing dialog's list: who can currently reach this one thing, names resolved. */
    @Test
    fun listSharesProjectsWhoCanReachOneInstance() {
        service.share(guest, tenant, "doc", "doc-20", "doc:read")
        service.share(owner, tenant, "doc", "doc-20", "doc:write")

        val shares = service.listShareVos("doc", "doc-20")
        assertEquals(setOf(guest, owner), shares.map { it.principalId }.toSet())
        assertTrue(shares.all { it.active })
        assertEquals("svc-ins-guest-Cq3", shares.first { it.principalId == guest }.principalName)
    }

    /**
     * The direction no role report can produce. Instance shares sit outside the role model on
     * purpose, so without this an offboarding review that walks roles is quietly incomplete —
     * missing precisely the access that was handed out informally.
     */
    @Test
    fun listSharesOfPrincipalAnswersWhatOnePersonWasGiven() {
        service.share(guest, tenant, "doc", "doc-21", "doc:read")
        service.share(guest, tenant, "report", "rep-9", "report:read")
        service.share(owner, tenant, "doc", "doc-22", "doc:read")

        val all = service.listSharesOfPrincipal(guest)
        assertEquals(setOf("doc-21", "rep-9"), all.map { it.instanceId }.toSet())
        assertTrue(all.none { it.principalId == owner }, "one principal's shares only")

        val docsOnly = service.listSharesOfPrincipal(guest, "doc")
        assertEquals(listOf("doc-21"), docsOnly.map { it.instanceId })
    }

    /**
     * The defect this closes: sharing validated nothing about its receiver. Any string was a
     * principal and the tenant was whatever the caller typed, which made "share this document" a
     * cross-tenant grant primitive — through the one boundary the rest of the model treats as hard.
     */
    @Test
    fun aShareMayNotCrossTheTenantBoundary() {
        val failure = assertFailsWith<IllegalArgumentException> {
            service.share(guest, "some-other-tenant", "doc", "doc-30", "doc:read")
        }
        assertTrue(failure.message!!.contains("cross-tenant"), failure.message!!)
    }

    /** ...and a recipient nobody has heard of is refused rather than silently granted. */
    @Test
    fun aShareToAnUnknownPrincipalIsRefused() {
        val failure = assertFailsWith<IllegalArgumentException> {
            service.share("ins10000-0000-0000-0000-0000000000ff", tenant, "doc", "doc-31", "doc:read")
        }
        assertTrue(failure.message!!.contains("does not exist"), failure.message!!)
    }

    /** The ordinary case still works, so the check is a boundary and not a wall. */
    @Test
    fun aShareWithinTheTenantStillWorks() {
        val id = service.share(guest, tenant, "doc", "doc-32", "doc:read")
        assertTrue(id.isNotBlank())
        assertTrue(decide(guest, "doc:read", "doc", "doc-32").permitted)
    }

    /** A withdrawn share disappears from both directions — an un-share has to actually un-share. */
    @Test
    fun aWithdrawnShareLeavesBothListings() {
        val id = service.share(guest, tenant, "doc", "doc-23", "doc:read")
        assertTrue(service.listShareVos("doc", "doc-23").isNotEmpty())

        assertTrue(service.unshare(id, "no longer needed"))

        assertTrue(service.listShareVos("doc", "doc-23").isEmpty())
        assertTrue(service.listSharesOfPrincipal(guest).none { it.instanceId == "doc-23" })
    }
}
