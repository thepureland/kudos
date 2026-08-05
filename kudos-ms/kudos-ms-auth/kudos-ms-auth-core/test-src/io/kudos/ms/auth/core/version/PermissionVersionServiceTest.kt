package io.kudos.ms.auth.core.version

import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.platform.authz.cache.PermissionGrantsByUserIdCache
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.auth.core.version.cache.PermissionVersionByUserIdCache
import io.kudos.ms.auth.core.version.event.PrincipalTokenEpochBumped
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * junit test for [io.kudos.ms.auth.core.version.service.impl.PermissionVersionService].
 *
 * Test data source: `PermissionVersionServiceTest.sql`
 *
 * The mechanism's whole value is that a token stops working the moment it should, and keeps working
 * when nothing relevant changed. Both halves are failure modes: too eager and every permission edit
 * logs the organisation out; too lax and a revocation silently does not take.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class PermissionVersionServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var api: IPermissionVersionApi

    @Resource
    private lateinit var versionCache: PermissionVersionByUserIdCache

    @Resource
    private lateinit var grantsCache: PermissionGrantsByUserIdCache

    @Resource
    private lateinit var roleUserService: IAuthRoleUserService

    /**
     * AFTER_COMMIT listeners do not fire inside these rolled-back tests, so the evictions a real
     * event would trigger are driven directly — same convention as RoleIdsByUserIdCacheTest. Both
     * caches are announced because the version is *derived* from the grants: evicting only the
     * version would recompute it from a stale input, which is a property of the test harness rather
     * than of the mechanism.
     */
    private fun announceRoleChange(roleId: String, userId: String) {
        grantsCache.on(AuthRoleUserRelationsChanged(roleId, listOf(userId)))
        versionCache.on(AuthRoleUserRelationsChanged(roleId, listOf(userId)))
    }

    private val user = "3f2c9b10-0000-0000-0000-0000000000a1"
    private val otherUser = "3f2c9b10-0000-0000-0000-0000000000a2"
    private val extraRole = "3f2c9b10-0000-0000-0000-0000000000b2"
    private val subject get() = SubjectRef.ofUser(user)

    @Test
    fun aTokenCarryingTheCurrentVersionIsAccepted() {
        val version = api.currentVersion(subject)
        assertTrue(version.isNotBlank())
        assertTrue(api.isCurrent(subject, version))
    }

    /**
     * A token that carries no version cannot be shown to be current. Treating "unknown" as "fine"
     * is how a revocation check quietly stops checking — and from here it is indistinguishable from
     * an attacker simply omitting the claim.
     */
    @Test
    fun aTokenWithNoVersionIsNeverCurrent() {
        assertFalse(api.isCurrent(subject, null))
        assertFalse(api.isCurrent(subject, ""))
        assertFalse(api.isCurrent(subject, "   "))
        assertFalse(api.isCurrent(subject, "0:deadbeefdeadbeef"))
    }

    /**
     * The administrative half: the permissions did not change at all, and every token must still
     * die. This is the leaked-credential case, which a purely derived version cannot express.
     */
    @Test
    fun forcedRevocationInvalidatesTokensWithoutAnyPermissionChange() {
        val before = api.currentVersion(subject)
        assertTrue(api.isCurrent(subject, before))

        val epoch = api.revokeAllTokens(user, "credential leaked")
        assertTrue(epoch >= 1)
        // AFTER_COMMIT listeners do not fire inside these rolled-back tests, so the eviction the
        // bump triggers is driven directly — same convention as RoleIdsByUserIdCacheTest.
        versionCache.on(PrincipalTokenEpochBumped(user, epoch, "credential leaked"))

        assertFalse(api.isCurrent(subject, before), "the old token must no longer be current")
        assertTrue(api.isCurrent(subject, api.currentVersion(subject)), "a freshly minted one must be")
    }

    /** Monotonic: a later change can never resurrect a token an administrator killed. */
    @Test
    fun theEpochOnlyEverIncreases() {
        val first = api.revokeAllTokens(user, "one")
        val second = api.revokeAllTokens(user, "two")
        val third = api.revokeAllTokens(user, "three")
        assertTrue(second > first && third > second)
    }

    /**
     * The derived half: granting a role changes what the subject may do, so tokens minted before it
     * are no longer describing reality.
     */
    @Test
    fun aPermissionChangeMovesTheVersion() {
        val before = api.currentVersion(subject)

        roleUserService.batchBind(extraRole, listOf(user))
        announceRoleChange(extraRole, user)

        assertNotEquals(before, api.currentVersion(subject))
    }

    /**
     * ...and the converse, which a naive counter gets wrong: re-binding a role the subject already
     * holds changes nothing about what they may do, so it must not log them out. A version that
     * moved here would make every routine administrative touch an outage.
     */
    @Test
    fun aChangeThatGrantsNothingNewLeavesTheVersionAlone() {
        val before = api.currentVersion(subject)

        roleUserService.batchBind(extraRole, listOf(user))
        announceRoleChange(extraRole, user)
        val afterFirst = api.currentVersion(subject)

        // Bind again — already bound, so the effective permission set is untouched.
        roleUserService.batchBind(extraRole, listOf(user))
        announceRoleChange(extraRole, user)

        assertEquals(afterFirst, api.currentVersion(subject))
        assertNotEquals(before, afterFirst)
    }

    /** Versions are per principal; one person's revocation must not touch anybody else's tokens. */
    @Test
    fun revocationIsScopedToOnePrincipal() {
        val otherBefore = api.currentVersion(SubjectRef.ofUser(otherUser))

        val epoch = api.revokeAllTokens(user, "scoped")
        versionCache.on(PrincipalTokenEpochBumped(user, epoch, "scoped"))

        assertTrue(api.isCurrent(SubjectRef.ofUser(otherUser), otherBefore))
    }
}
