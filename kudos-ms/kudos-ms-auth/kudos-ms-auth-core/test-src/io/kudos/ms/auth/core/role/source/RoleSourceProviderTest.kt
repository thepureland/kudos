package io.kudos.ms.auth.core.role.source

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.platform.authz.cache.PermissionGrantsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for [IRoleSourceProvider] / [RoleSourceRegistry].
 *
 * Test data source: `RoleSourceProviderTest.sql`
 *
 * Models the business shape the SPI exists for: an HR-style system where people are assigned
 * *positions* and the roles follow, with no `auth_role_user` row ever written. The two properties
 * that make it usable rather than merely present are that a contributed role reaches the decision
 * point, and that it is subject to every existing rule rather than exempt from them.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
@Import(RoleSourceProviderTest.PositionSourceConfiguration::class)
class RoleSourceProviderTest : RdbAndRedisCacheTestBase() {

    companion object {
        const val APPROVER = "9a1f5c22-0000-0000-0000-0000000000e1"
        const val NOBODY = "9a1f5c22-0000-0000-0000-0000000000e2"
        const val APPROVER_ROLE = "9a1f5c22-0000-0000-0000-0000000000b1"
        const val DEACTIVATED_ROLE = "9a1f5c22-0000-0000-0000-0000000000b2"

        /** Flipped by a test to prove a failing source narrows rather than breaks. */
        var explode = false
    }

    /** An application's whole integration: one bean, two methods. */
    @TestConfiguration
    open class PositionSourceConfiguration {
        @Bean
        open fun positionRoleSource(): IRoleSourceProvider = object : IRoleSourceProvider {
            override fun sourceName(): String = "position"
            override fun roleIdsOf(principalId: String): Set<String> {
                if (explode) throw IllegalStateException("HR system unreachable")
                // "Regional approver" and "auditor" come with the post, not with a grant.
                return if (principalId == APPROVER) setOf(APPROVER_ROLE, DEACTIVATED_ROLE) else emptySet()
            }
        }
    }

    @Resource
    private lateinit var registry: RoleSourceRegistry

    @Resource
    private lateinit var roleIdsCache: RoleIdsByUserIdCache

    @Resource
    private lateinit var grantsCache: PermissionGrantsByUserIdCache

    @Resource
    private lateinit var decisionApi: IAuthzDecisionApi

    private fun reset() {
        explode = false
        roleIdsCache.on(ExternalRoleSourceChanged(listOf(APPROVER, NOBODY), "position"))
        grantsCache.on(ExternalRoleSourceChanged(listOf(APPROVER, NOBODY), "position"))
    }

    @Test
    fun theRegistrySeesTheContributedSource() {
        reset()
        assertFalse(registry.isEmpty())
        assertTrue(registry.sourceNames().contains("position"))
        assertTrue(registry.contributedRoleIds(APPROVER).contains(APPROVER_ROLE))
        assertTrue(registry.contributedRoleIds(NOBODY).isEmpty())
    }

    /**
     * The point of the SPI: the permission arrives without anybody writing an `auth_role_user` row,
     * so there is never a window between the position ending and a sync job catching up.
     */
    @Test
    fun aContributedRoleReachesTheDecisionPoint() {
        reset()
        assertTrue(roleIdsCache.getRoleIds(APPROVER).contains(APPROVER_ROLE))
        assertTrue(
            decisionApi.decide(AuthzRequest(SubjectRef.ofUser(APPROVER), "rs:invoice:approve")).permitted,
        )
        assertFalse(
            decisionApi.decide(AuthzRequest(SubjectRef.ofUser(NOBODY), "rs:invoice:approve")).permitted,
            "somebody without the position holds nothing",
        )
    }

    /**
     * A source contributes roles; it does not exempt them from anything. A deactivated role
     * contributes nothing however it was reached — otherwise "turn this role off" would mean
     * something different depending on how people came to hold it.
     */
    @Test
    fun aContributedRoleIsStillSubjectToDeactivation() {
        reset()
        assertFalse(
            roleIdsCache.getRoleIds(APPROVER).contains(DEACTIVATED_ROLE),
            "the source offered it; `active = false` still removes it",
        )
        assertFalse(
            decisionApi.decide(AuthzRequest(SubjectRef.ofUser(APPROVER), "rs:invoice:void")).permitted,
        )
    }

    /**
     * An external system being down must not become "this person has no permissions at all" —
     * that would manufacture an outage out of a dependency's outage. The contributed roles are
     * absent until it recovers, which is strictly narrower than the truth: the safe direction.
     */
    @Test
    fun aFailingSourceNarrowsRatherThanBreaks() {
        reset()
        explode = true
        roleIdsCache.on(ExternalRoleSourceChanged(listOf(APPROVER), "position"))
        grantsCache.on(ExternalRoleSourceChanged(listOf(APPROVER), "position"))

        // The direct grant this principal also holds is unaffected; only the contributed one is gone.
        val roles = roleIdsCache.getRoleIds(APPROVER)
        assertFalse(roles.contains(APPROVER_ROLE))
        assertTrue(
            decisionApi.decide(AuthzRequest(SubjectRef.ofUser(APPROVER), "rs:invoice:read")).permitted,
            "the directly granted role still works",
        )
    }
}
