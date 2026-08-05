package io.kudos.ms.auth.core.principal

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.platform.authz.cache.PermissionGrantsByUserIdCache
import io.kudos.ms.auth.core.principal.spi.IPrincipalDirectory
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.principal.spi.PrincipalFacts
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end test for machine principals.
 *
 * Test data source: `MachinePrincipalTest.sql`
 *
 * `principal_type` has been a column and a field for a while, but every path resolved a principal by
 * reading `user_account` — so a service account could be *modelled* and then never granted anything,
 * because the existence check refused it. Machine access then has to be built around the
 * authorization system rather than inside it, which is the failure this locks against.
 *
 * The test registers a throwaway `SERVICE` directory, exactly as a deployment with service accounts
 * would, and asserts the whole path works with no human account anywhere in it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
@Import(MachinePrincipalTest.ServiceDirectoryConfiguration::class)
class MachinePrincipalTest : RdbAndRedisCacheTestBase() {

    companion object {
        // Full-width ids: `user_id` is CHAR(36), so anything shorter comes back space-padded.
        const val SERVICE_PRINCIPAL = "8d4e1a70-0000-0000-0000-0000000000e1"
        const val DISABLED_SERVICE = "8d4e1a70-0000-0000-0000-0000000000e9"
        const val TENANT = "mp-tenant-8d4e1a70"
        const val OTHER_TENANT = "mp-tenant-other-8d4e1a70"
    }

    /** A deployment with service accounts contributes exactly this: one bean, two methods. */
    @TestConfiguration
    open class ServiceDirectoryConfiguration {
        @Bean
        open fun serviceDirectory(): IPrincipalDirectory = object : IPrincipalDirectory {
            override fun principalType(): String = "SERVICE"
            override fun find(principalId: String): PrincipalFacts? = when (principalId) {
                SERVICE_PRINCIPAL -> PrincipalFacts(principalId, "SERVICE", TENANT, true, "nightly-etl")
                DISABLED_SERVICE -> PrincipalFacts(principalId, "SERVICE", TENANT, false, "retired-bot")
                else -> null
            }
        }
    }

    @Resource
    private lateinit var policyService: IAuthGrantPolicyService

    @Resource
    private lateinit var decisionApi: IAuthzDecisionApi

    @Resource
    private lateinit var registry: PrincipalDirectoryRegistry

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var grantsCache: PermissionGrantsByUserIdCache

    private val serviceRole = "8d4e1a70-0000-0000-0000-0000000000b1"
    private val otherTenantRole = "8d4e1a70-0000-0000-0000-0000000000b2"

    @Test
    fun aContributedDirectoryMakesItsPrincipalKindVisible() {
        assertTrue(registry.registeredTypes().containsAll(listOf("SERVICE", "USER")))
        assertFalse(registry.onlyHumanPrincipals())
        assertEquals("nightly-etl", registry.find(SERVICE_PRINCIPAL, "SERVICE")?.displayName)
    }

    /**
     * The defect this closes: before the directory existed, this candidate was rejected with
     * "principal does not exist" purely because it was not a row in `user_account`.
     */
    @Test
    fun aServicePrincipalMayBeGrantedARole() {
        val rejections = policyService.screenGrants(
            listOf(GrantCandidate(serviceRole, SERVICE_PRINCIPAL, principalType = "SERVICE")),
        )
        assertTrue(rejections.isEmpty(), "unexpected rejections: ${rejections.map { it.reason }}")
    }

    /** The tenant boundary is the same hard boundary for a machine as for a person. */
    @Test
    fun crossTenantGrantsAreRefusedForMachinesToo() {
        val rejections = policyService.screenGrants(
            listOf(GrantCandidate(otherTenantRole, SERVICE_PRINCIPAL, principalType = "SERVICE")),
        )
        assertEquals(1, rejections.size)
        assertTrue(rejections.single().reason.contains("cross-tenant"))
    }

    /** A retired service account must not be able to receive new access. */
    @Test
    fun aDisabledMachinePrincipalMayNotBeGranted() {
        val rejections = policyService.screenGrants(
            listOf(GrantCandidate(serviceRole, DISABLED_SERVICE, principalType = "SERVICE")),
        )
        assertEquals(1, rejections.size)
        assertTrue(rejections.single().reason.contains("disabled"))
    }

    /** A principal no directory claims is refused — granting to an unknown receiver must be loud. */
    @Test
    fun anUnknownPrincipalIsStillRefused() {
        val rejections = policyService.screenGrants(
            listOf(GrantCandidate(serviceRole, "8d4e1a70-0000-0000-0000-00000000dead", principalType = "SERVICE")),
        )
        assertEquals(1, rejections.size)
        assertTrue(rejections.single().reason.contains("does not exist"))
    }

    /**
     * The other end: once granted, the decision point answers for a SERVICE subject exactly as it
     * does for a person. Nothing about the algebra is human-specific, and this proves it rather than
     * assuming it.
     */
    @Test
    fun theDecisionPointAnswersForAMachineSubject() {
        val subject = SubjectRef(SERVICE_PRINCIPAL, "SERVICE")
        assertFalse(
            decisionApi.decide(AuthzRequest(subject, "mp:etl:run")).permitted,
            "nothing granted yet, so default deny",
        )

        authRoleUserDao.insert(AuthRoleUser {
            this.roleId = serviceRole
            this.userId = SERVICE_PRINCIPAL
            this.principalType = "SERVICE"
        })
        // "Holds nothing" is a cached answer like any other, so granting behind the cache's back
        // needs the same announcement a real write would publish. AFTER_COMMIT listeners do not
        // fire inside these rolled-back tests, so it is driven directly.
        grantsCache.on(AuthRoleUserRelationsChanged(serviceRole, listOf(SERVICE_PRINCIPAL)))

        assertTrue(
            decisionApi.decide(AuthzRequest(subject, "mp:etl:run")).permitted,
            "a machine principal holding the role exercises it like anyone else",
        )
        assertFalse(
            decisionApi.decide(AuthzRequest(subject, "mp:etl:delete")).permitted,
            "and is refused what the role does not carry",
        )
    }
}
