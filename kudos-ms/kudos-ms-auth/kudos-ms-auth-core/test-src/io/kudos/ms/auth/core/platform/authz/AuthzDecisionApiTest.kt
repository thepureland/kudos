package io.kudos.ms.auth.core.platform.authz

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.platform.authz.init.properties.AuthzProperties
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import io.kudos.ms.auth.core.platform.authz.cache.PermissionGrantsByUserIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * junit test for the decision algebra.
 *
 * Test data source: `AuthzDecisionApiTest.sql`.
 *
 * The cases below are the algebra's contract, in evaluation order: platform short circuit, wildcard
 * ALLOW, DENY overriding it, conditions gating a binding, code resolution through a resource id, and
 * default deny for everything else.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthzDecisionApiTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authzDecisionApi: IAuthzDecisionApi

    @Resource
    private lateinit var authzProperties: AuthzProperties

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var permissionGrantsByUserIdCache: PermissionGrantsByUserIdCache

    private val staff = SubjectRef.ofUser("dec10000-0000-0000-0000-000000000001")
    private val auditor = SubjectRef.ofUser("dec10000-0000-0000-0000-000000000002")
    private val orphan = SubjectRef.ofUser("dec10000-0000-0000-0000-000000000003")
    private val platform = SubjectRef.ofUser("dec10000-0000-0000-0000-000000000004")
    private val nobody = SubjectRef.ofUser("dec10000-0000-0000-0000-000000000005")

    private fun decide(subject: SubjectRef, code: String, context: Map<String, Any?> = emptyMap()) =
        authzDecisionApi.decide(AuthzRequest(subject, code, context))

    // -------------------- 1. platform short circuit --------------------

    @Test
    fun platformAdmin_shortCircuitsButIsStillAttributed() {
        val previous = authzProperties.platformAdminRoleCodes
        val previousTenants = authzProperties.platformAdminTenantIds
        authzProperties.platformAdminRoleCodes = setOf("KUDOS_PLATFORM_ADMIN_Kq2m")
        authzProperties.platformAdminTenantIds = setOf("tenant-authz-1-Kq2m")
        try {
            val decision = decide(platform, "anything:at:all")
            assertTrue(decision.permitted)
            // The bypass exists in every real system; the point is that it is *this* one, visible in
            // the decision rather than hidden in an if-statement somewhere.
            assertEquals(AuthzDecision.Reason.PLATFORM_ADMIN, decision.reason)
        } finally {
            authzProperties.platformAdminRoleCodes = previous
            authzProperties.platformAdminTenantIds = previousTenants
        }
    }

    @Test
    fun platformAdminRoles_areNotSpecialWhenNotConfigured() {
        val previous = authzProperties.platformAdminRoleCodes
        val previousTenants = authzProperties.platformAdminTenantIds
        authzProperties.platformAdminRoleCodes = emptySet()
        try {
            val decision = decide(platform, "anything:at:all")
            assertFalse(decision.permitted, "an unconfigured framework must ship no magic role")
            assertEquals(AuthzDecision.Reason.DENIED_BY_DEFAULT, decision.reason)
        } finally {
            authzProperties.platformAdminRoleCodes = previous
            authzProperties.platformAdminTenantIds = previousTenants
        }
    }

    // -------------------- 2..4. collect / condition / merge --------------------

    @Test
    fun wildcardGrant_permitsEveryCodeBelowIt() {
        val decision = decide(staff, "sys:user:read")
        assertTrue(decision.permitted)
        assertEquals(AuthzDecision.Reason.ALLOWED_BY_GRANT, decision.reason)
        assertEquals("sys:user:*", decision.matchedCode, "the evidence must name the rule that permitted it")
    }

    @Test
    fun denyBeatsAWiderAllow() {
        // The exception case that has no expression at all without DENY in the model: everything
        // under sys:user except the one dangerous action.
        val decision = decide(staff, "sys:user:delete")
        assertFalse(decision.permitted)
        assertEquals(AuthzDecision.Reason.DENIED_BY_RULE, decision.reason)
        assertEquals("sys:user:delete", decision.matchedCode)
    }

    @Test
    fun conditionalGrant_appliesOnlyWhenTheConditionHolds() {
        assertTrue(decide(staff, "msg:template:read", mapOf("ip" to "10.1.2.3")).permitted)

        val outside = decide(staff, "msg:template:read", mapOf("ip" to "192.168.0.9"))
        assertFalse(outside.permitted)
        // A condition that does not hold makes the binding absent, not denying — so the answer is
        // the default, not "denied by rule".
        assertEquals(AuthzDecision.Reason.DENIED_BY_DEFAULT, outside.reason)

        assertFalse(
            decide(staff, "msg:template:read").permitted,
            "a caller who supplies no context cannot satisfy a condition that asks for one",
        )
    }

    /**
     * The asymmetry that makes conditional DENY trustworthy: a condition judged **false** stands the
     * DENY down, but a condition that **cannot be judged** keeps it in force.
     *
     * The distinction is not pedantry — the context map is entirely the caller's to supply. Under
     * the old rule ("any evaluation failure makes the binding absent") a caller could lift every
     * conditional restriction simply by not sending the attribute it asks about, which turned each
     * conditional DENY into a politely worded suggestion.
     */
    @Test
    fun aConditionalDenyWhoseEvidenceIsMissingStaysInForce() {
        // staff holds sys:user:* unconditionally; this DENY on one code beneath it is gated on a
        // context attribute the fixture's other tests never send.
        val denyRow = authRoleResourceDao.insert(AuthRoleResource {
            this.roleId = STAFF_ROLE
            this.permissionCode = "sys:user:export"
            this.effect = PermissionEffect.DENY.name
            this.condition = "region=cn"
        })
        permissionGrantsByUserIdCache.on(AuthRoleUserRelationsChanged(STAFF_ROLE, listOf(staff.principalId)))
        try {
            val judgedFalse = decide(staff, "sys:user:export", mapOf("region" to "eu"))
            assertTrue(judgedFalse.permitted, "a DENY whose condition was judged false is absent")

            val judgedTrue = decide(staff, "sys:user:export", mapOf("region" to "cn"))
            assertFalse(judgedTrue.permitted)
            assertEquals(AuthzDecision.Reason.DENIED_BY_RULE, judgedTrue.reason)

            val undecidable = decide(staff, "sys:user:export")
            assertFalse(
                undecidable.permitted,
                "a DENY whose condition could not be judged must stay in force — withholding the " +
                    "evidence must never lift the restriction",
            )
            assertEquals(AuthzDecision.Reason.DENIED_BY_RULE, undecidable.reason)
        } finally {
            authRoleResourceDao.deleteById(denyRow)
            permissionGrantsByUserIdCache.on(AuthRoleUserRelationsChanged(STAFF_ROLE, listOf(staff.principalId)))
        }
    }

    @Test
    fun resourceIdBinding_resolvesThroughTheRegisteredCode() {
        val decision = decide(auditor, "audit:log:read")
        assertTrue(decision.permitted, "a legacy resource-id binding still works once its resource has a code")
        assertEquals("audit:log:read", decision.matchedCode)
    }

    @Test
    fun resourceWithoutACode_grantsNothing() {
        // Honest behaviour during the migration: an unregistered permission point is not enforceable,
        // and pretending otherwise would be the kind of silent hole the whole design avoids.
        val decision = decide(orphan, "legacy:thing:use")
        assertFalse(decision.permitted)
        assertEquals(AuthzDecision.Reason.DENIED_BY_DEFAULT, decision.reason)
        assertTrue(authzDecisionApi.resolveGrants(orphan).isEmpty())
    }

    @Test
    fun defaultDeny_forUnknownCodesAndRolelessSubjects() {
        assertFalse(decide(staff, "msg:campaign:launch").permitted)
        assertFalse(decide(nobody, "sys:user:read").permitted)
        assertEquals(AuthzDecision.Reason.DENIED_BY_DEFAULT, decide(nobody, "sys:user:read").reason)
    }

    @Test
    fun blankCode_isRefusedRatherThanMatchedByAWildcard() {
        val decision = decide(staff, "")
        assertFalse(decision.permitted)
        assertEquals(AuthzDecision.Reason.DENIED_BY_DEFAULT, decision.reason)
    }

    // -------------------- projections --------------------

    @Test
    fun permissionCodes_mergesDenyOutOfTheAllowSet() {
        val codes = authzDecisionApi.permissionCodes(staff)
        assertTrue(codes.contains("sys:user:*"), "wildcards are returned as-is, not expanded")
        assertFalse(codes.contains("sys:user:delete"), "a DENY removes what it covers")
        assertFalse(codes.contains("msg:template:read"), "a conditional grant needs its context")

        val inOffice = authzDecisionApi.permissionCodes(staff, mapOf("ip" to "10.1.2.3"))
        assertTrue(inOffice.contains("msg:template:read"))
    }

    @Test
    fun resolveGrants_keepsAllowAndDenySeparate() {
        // The explain view needs the unmerged truth; permissionCodes() can no longer express "why".
        val grants = authzDecisionApi.resolveGrants(staff)
        assertTrue(grants.any { it.permissionCode == "sys:user:*" && it.effect == PermissionEffect.ALLOW })
        assertTrue(grants.any { it.permissionCode == "sys:user:delete" && it.effect == PermissionEffect.DENY })
        assertTrue(grants.any { it.condition == "ip=10.0.0.0/8" })
        assertTrue(grants.all { it.roleId != null }, "every grant must name the role it came from")
    }

    /**
     * "This principal holds nothing" must be cached like any other answer.
     *
     * It used to be excluded (`unless = result.isEmpty()`), which meant the decision path — the one
     * place this module promises is a pure cache hit — went to the database on *every* request made
     * by anyone holding no grants. That is a freshly created account, one whose roles were just
     * revoked, a misconfigured client retrying: precisely the callers a system should be able to
     * refuse without touching storage. It also made them the first to break when the cache tier went
     * down, while permitted users carried on from their cached entries.
     *
     * Proven the same way as the other cache tests: change the backing rows without publishing an
     * event, and assert the answer does not move. If the negative result were not cached, the new
     * grant would show up immediately.
     */
    @Test
    fun aPrincipalHoldingNothingIsCachedRatherThanReResolved() {
        assertTrue(authzDecisionApi.resolveGrants(nobody).isEmpty())

        val added = authRoleResourceDao.insert(AuthRoleResource {
            this.roleId = STAFF_ROLE
            this.permissionCode = "sys:probe:negativeCache"
            this.effect = PermissionEffect.ALLOW.name
        })
        val bound = authRoleUserDao.insert(AuthRoleUser {
            this.roleId = STAFF_ROLE
            this.userId = nobody.principalId
        })
        try {
            assertTrue(
                authzDecisionApi.resolveGrants(nobody).isEmpty(),
                "the empty answer must have been cached, not recomputed from the database",
            )
        } finally {
            authRoleUserDao.deleteById(bound)
            authRoleResourceDao.deleteById(added)
        }
    }

    /** ...and caching it must not make it permanent: the ordinary invalidation still reaches it. */
    @Test
    fun aCachedEmptyAnswerIsStillInvalidated() {
        assertTrue(authzDecisionApi.resolveGrants(nobody).isEmpty())

        val added = authRoleResourceDao.insert(AuthRoleResource {
            this.roleId = STAFF_ROLE
            this.permissionCode = "sys:probe:invalidation"
            this.effect = PermissionEffect.ALLOW.name
        })
        val bound = authRoleUserDao.insert(AuthRoleUser {
            this.roleId = STAFF_ROLE
            this.userId = nobody.principalId
        })
        try {
            // AFTER_COMMIT listeners do not fire inside these rolled-back tests, so the eviction is
            // driven directly — same convention as RoleIdsByUserIdCacheTest.
            permissionGrantsByUserIdCache.on(AuthRoleUserRelationsChanged(STAFF_ROLE, listOf(nobody.principalId)))
            assertTrue(
                authzDecisionApi.resolveGrants(nobody).isNotEmpty(),
                "once invalidated, the principal's new grants must be visible",
            )
        } finally {
            authRoleUserDao.deleteById(bound)
            authRoleResourceDao.deleteById(added)
        }
    }

    private companion object {
        const val STAFF_ROLE = "dec20000-0000-0000-0000-000000000001"
    }
}
