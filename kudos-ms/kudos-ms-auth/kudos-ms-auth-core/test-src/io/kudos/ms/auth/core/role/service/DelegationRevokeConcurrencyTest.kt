package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.common.delegation.vo.RoleGrantRequest
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Does the revocation cascade catch a delegation that is happening at the same instant?
 *
 * Test data source: `DelegationRevokeConcurrencyTest.sql`
 *
 * **The window under test.** Delegation is "read the operator's grant (live, delegable) → insert
 * children under it"; revocation is "read the subtree → mark it revoked". With no common lock, a
 * delegation racing the revocation of its operator can land its children *after* the cascade's
 * snapshot: the parent ends up revoked, the children stay live, and no future cascade will ever
 * find them again — authority with no traceable source. The same write-skew shape as the SoD race
 * (see `SoDConcurrencyTest`), on the other side of the ledger: that one minted a forbidden
 * combination, this one mints a permission that survives its own revocation.
 *
 * Closed by both sides serialising on the operator's principal row: `screenGrants` locks the
 * operator along with the recipients, and `revoke`/`unbind` lock every holder in the subtree before
 * reading it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DelegationRevokeConcurrencyTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var roleUserService: IAuthRoleUserService

    @Resource
    private lateinit var grantPolicyService: IAuthGrantPolicyService

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var transactionManager: PlatformTransactionManager

    private val role = "d1620000-0000-0000-0000-000000000001"
    private val boss = "d1610000-0000-0000-0000-000000000001"
    private val recipient = "d1610000-0000-0000-0000-000000000002"
    private val bossGrant = "d1650000-0000-0000-0000-000000000001"

    /** Sequential control, one way round: revoked first, the delegation must be refused. */
    @Test
    fun sequentially_delegationFromARevokedGrantIsRefused() {
        try {
            committedOffThread { roleUserService.revoke(bossGrant, "sequential control") }
            val refused = offThread {
                runCatching {
                    roleUserService.grant(RoleGrantRequest(role, listOf(recipient)), boss)
                }.isFailure
            }
            assertTrue(refused, "boss's grant is revoked, so delegating from it must be refused")
        } finally {
            cleanUp()
        }
    }

    /** Sequential control, the other way: delegated first, the cascade must take the child with it. */
    @Test
    fun sequentially_revocationCascadesOverAnExistingChild() {
        try {
            committedOffThread { roleUserService.grant(RoleGrantRequest(role, listOf(recipient)), boss) }
            committedOffThread { roleUserService.revoke(bossGrant, "sequential control") }
            assertTrue(orphans().isEmpty(), "the child was present before the cascade, so it must be revoked")
        } finally {
            cleanUp()
        }
    }

    /**
     * The race left to timing: both service calls released at the same instant, repeatedly.
     *
     * Whichever side commits first, the invariant is the same — either the delegation was refused
     * (operator already revoked) or the child was cascaded (it existed when the subtree was read).
     * A live child under the revoked parent is the defect.
     */
    @Test
    fun concurrently_noLiveChildMaySurviveUnderARevokedParent() {
        val attempts = 20
        var orphaned = 0

        repeat(attempts) { round ->
            try {
                val start = CountDownLatch(1)
                val done = CountDownLatch(2)
                val pool = Executors.newFixedThreadPool(2)
                listOf(
                    { roleUserService.grant(RoleGrantRequest(role, listOf(recipient)), boss); Unit },
                    { roleUserService.revoke(bossGrant, "race round ${round}"); Unit },
                ).forEach { action ->
                    pool.submit {
                        start.await()
                        runCatching(action)
                        done.countDown()
                    }
                }
                start.countDown()
                done.await(30, TimeUnit.SECONDS)
                pool.shutdown()

                if (orphans().isNotEmpty()) orphaned++
            } finally {
                cleanUp()
            }
        }

        println("[delegation race] ${orphaned}/${attempts} rounds left a live child under a revoked parent")
        assertTrue(
            orphaned == 0,
            "in ${orphaned}/${attempts} rounds a delegated grant stayed live under its revoked " +
                "parent — the cascade's snapshot was taken before the delegation landed, and " +
                "nothing will ever revoke that child now.",
        )
    }

    /**
     * The decisive test: the delegation's window held open on purpose while a real revocation runs.
     *
     * The thread below performs exactly the two steps the delegation write path performs — screen
     * (which under the fix takes the operator's lock), then insert — but pauses between them, which
     * is the state two simultaneous requests can always reach. The real `revoke` is started inside
     * the pause. If the two sides share no lock, the revocation completes during the pause, its
     * cascade sees no children, and the insert then lands an orphan. With the operator lock, the
     * revocation cannot read the subtree until the delegation commits, so the child it cascades is
     * precisely the one inserted here.
     */
    @Test
    fun withTheWindowHeldOpen_theCascadeStillCatchesTheChild() {
        try {
            val screened = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)

            val delegating = pool.submit {
                TransactionTemplate(transactionManager).apply {
                    propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
                }.execute {
                    // Step 1 — what grant() does first. Under the fix this takes boss's lock.
                    val rejections = grantPolicyService.screenGrants(
                        listOf(GrantCandidate(role, recipient, operatorId = boss, requestedDepth = 0)),
                    )
                    screened.countDown()
                    // Hold the window open. A revocation with no common lock finishes inside this
                    // pause; one that locks the operator is still waiting when we commit.
                    Thread.sleep(3000)
                    // Step 2 — what grant() does second, having decided.
                    if (rejections.isEmpty()) {
                        authRoleUserDao.insert(AuthRoleUser {
                            this.roleId = role
                            this.userId = recipient
                            this.grantedBy = boss
                            this.parentGrantId = bossGrant
                            this.delegableDepth = 0
                            this.revoked = false
                        })
                    }
                }
            }
            // The revocation fired into the held-open window has exactly two safe outcomes: it
            // blocks on the operator's lock (and, if the pause outlasts the database's lock wait,
            // fails loudly), or it runs after the delegation commits and cascades the child. The
            // unsafe outcome — the one the unfixed code produces — is that it *completes* inside
            // the pause: nothing blocked it, its cascade saw no children, and the insert then
            // lands an orphan.
            val revoking = pool.submit<Result<Int>> {
                screened.await(10, TimeUnit.SECONDS)
                runCatching { roleUserService.revoke(bossGrant, "window held open") }
            }
            delegating.get(60, TimeUnit.SECONDS)
            val firstAttempt = revoking.get(60, TimeUnit.SECONDS)
            pool.shutdown()

            firstAttempt.exceptionOrNull()?.let {
                println("[delegation window] revoke inside the window failed loudly (${it::class.simpleName}) — retrying now the window is closed")
                offThread { roleUserService.revoke(bossGrant, "window held open, retried") }
            }

            assertTrue(
                committedOffThread { authRoleUserDao.get(bossGrant)?.revoked == true },
                "the revocation must eventually land — a lock that refuses forever is an outage, not safety",
            )
            assertTrue(
                orphans().isEmpty(),
                "with the delegation's read-to-write window held open across a real revocation, " +
                    "the child landed after the cascade's snapshot and survived it: a live grant " +
                    "under a revoked parent, which nothing will ever revoke again.",
            )
        } finally {
            cleanUp()
        }
    }

    // -------------------- harness --------------------
    // Same conventions as SoDConcurrencyTest, for the same reasons: the test's own transaction is
    // rolled back and invisible to the workers, and the pool runs with auto-commit off, so both
    // observation and cleanup must happen off-thread inside committed transactions.

    private fun <T> offThread(block: () -> T): T {
        val pool = Executors.newSingleThreadExecutor()
        try {
            return pool.submit<T> { block() }.get(60, TimeUnit.SECONDS)
        } finally {
            pool.shutdown()
        }
    }

    private fun <T> committedOffThread(block: () -> T): T = offThread {
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }.execute { block() }!!
    }

    /** Live grants whose parent is boss's revoked grant — the rows the cascade must never miss. */
    private fun orphans(): List<AuthRoleUser> = committedOffThread {
        val parentRevoked = authRoleUserDao.get(bossGrant)?.revoked == true
        if (!parentRevoked) emptyList()
        else authRoleUserDao.searchGrantsByRoleId(role)
            .filter { it.parentGrantId == bossGrant && it.revoked != true }
    }

    /** Restore boss's grant and remove whatever the round delegated, whether it was cascaded or not. */
    private fun cleanUp() {
        committedOffThread {
            authRoleUserDao.get(bossGrant)?.let { grant ->
                grant.revoked = false
                grant.revokeReason = null
                authRoleUserDao.update(grant)
            }
            authRoleUserDao.searchGrantsByRoleId(role)
                .filter { it.userId == recipient }
                .forEach { authRoleUserDao.deleteById(it.id) }
        }
    }
}
