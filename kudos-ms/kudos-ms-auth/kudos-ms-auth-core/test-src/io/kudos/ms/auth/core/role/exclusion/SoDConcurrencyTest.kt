package io.kudos.ms.auth.core.role.exclusion

import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import jakarta.annotation.Resource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Does separation of duties survive two administrators acting at the same instant?
 *
 * Test data source: `SoDConcurrencyTest.sql`
 *
 * **The window under test.** Every grant write does the same three things: read the principal's
 * effective role set, check it against the exclusion rules, insert. That is a read-then-write, and
 * the only database constraint behind it is `unique(role_id, user_id)` — which stops the *same*
 * role being bound twice and says nothing about two *different* roles that must never be held
 * together. Under READ COMMITTED, two transactions binding the exclusive halves each read a state
 * in which the other has not committed yet, so both can pass screening and both can commit. That is
 * textbook write skew, and if it reproduces here then SoD is advisory rather than enforced.
 *
 * **Why it matters more than an ordinary race.** Separation of duties exists precisely for the
 * adversarial case. "Rare and needs two simultaneous requests" is a weak defence against someone who
 * can issue two simultaneous requests on purpose.
 *
 * This test is deliberately written to *report* rather than to pass by construction: it runs the
 * race repeatedly and asserts on what actually landed in the database.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SoDConcurrencyTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var roleUserService: IAuthRoleUserService

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var grantPolicyService: IAuthGrantPolicyService

    @Resource
    private lateinit var transactionManager: PlatformTransactionManager

    private val user = "50d00000-0000-0000-0000-0000000000e1"
    private val maker = "50d00000-0000-0000-0000-0000000000a1"
    private val checker = "50d00000-0000-0000-0000-0000000000a2"

    /**
     * Each test's writes commit for real, so a test that fails midway can leave `user` still holding
     * maker or checker. JUnit does not fix the method order, so the leftover would land on whichever
     * test runs next and fail it instead — the flakiness this class showed. Start from a clean slate
     * rather than relying only on the per-test `finally`.
     */
    @BeforeTest
    fun resetBindings() = cleanUp()

    /** Sequential control: the rule itself works when the two writes do not overlap. */
    @Test
    fun sequentially_theSecondExclusiveRoleIsRefused() {
        try {
            committedOffThread { roleUserService.batchBind(maker, listOf(user)) }
            val rejected = offThread { runCatching { roleUserService.batchBind(checker, listOf(user)) }.isFailure }
            assertTrue(rejected, "holding maker, the checker bind must be refused")
            assertTrue(!heldRoles().containsAll(setOf(maker, checker)))
        } finally {
            cleanUp()
        }
    }

    /**
     * The race. Two threads, released together, each binding one half of an exclusive pair.
     *
     * Repeated because a race that reproduces one time in ten is still a race; a single green run
     * would prove nothing.
     */
    @Test
    fun concurrently_bothExclusiveRolesMustNotLand() {
        val attempts = 40
        var bothLanded = 0
        val outcomes = mutableListOf<String>()

        repeat(attempts) { round ->
            try {
                val start = CountDownLatch(1)
                val done = CountDownLatch(2)
                val successes = AtomicInteger()
                val pool = Executors.newFixedThreadPool(2)
                listOf(maker, checker).forEach { roleId ->
                    pool.submit {
                        start.await()
                        // Each thread gets its own transaction: the service method is @Transactional
                        // and this is a fresh thread, which is exactly the production shape of two
                        // administrators clicking at once.
                        runCatching { roleUserService.batchBind(roleId, listOf(user)) }
                            .onSuccess { successes.incrementAndGet() }
                        done.countDown()
                    }
                }
                start.countDown()
                done.await(30, TimeUnit.SECONDS)
                pool.shutdown()

                val held = heldRoles()
                val both = held.containsAll(setOf(maker, checker))
                if (both) bothLanded++
                if (both) outcomes += "round ${round}: BREACH — writes-accepted=${successes.get()}, held=${held.size}"
            } finally {
                cleanUp()
            }
        }

        println("[SoD race] ${bothLanded}/${attempts} rounds ended with both exclusive roles held")
        outcomes.forEach { println("[SoD race] ${it}") }
        assertTrue(
            bothLanded == 0,
            "separation of duties was breached in ${bothLanded}/${attempts} concurrent rounds: the " +
                "principal ended up holding both halves of an exclusive pair. Screening reads the " +
                "effective role set and then inserts, with no database constraint or lock spanning " +
                "the two, so concurrent binds of the two halves can each observe the other's absence.",
        )
    }

    /**
     * The decisive test: the same two steps the service performs, in the same order, with the
     * interleaving pinned instead of left to luck.
     *
     * The timing test above leaves the window to chance and did not reproduce on H2 in 40 rounds —
     * which shows the race is *narrow*, not that it is absent. This one removes the luck: both
     * transactions complete their screening before either inserts, which is precisely the state two
     * simultaneous requests can reach. If SoD were enforced by anything other than the optimistic
     * read, the second insert would still fail.
     *
     * A pinned interleaving is a fair model here because nothing in the write path claims to
     * prevent it — no lock is taken, no constraint spans the two roles, and the screening reads no
     * later than the insert writes.
     */
    @Test
    fun withTheWindowHeldOpen_bothExclusiveRolesLand() {
        try {
            val screened = CountDownLatch(2)
            val pool = Executors.newFixedThreadPool(2)
            val landed = AtomicInteger()
            val screenedOk = AtomicInteger()

            listOf(maker, checker).forEach { roleId ->
                pool.submit {
                    // A real transaction per thread — the connection pool runs with auto-commit off,
                    // so a write outside one is quietly discarded rather than persisted. This is the
                    // production shape: the service method is @Transactional.
                    val template = TransactionTemplate(transactionManager).apply {
                        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
                    }
                    runCatching {
                        template.execute {
                            // Step 1 — exactly what the service does first: is this grant admissible?
                            val rejections = grantPolicyService.screenGrants(listOf(GrantCandidate(roleId, user)))
                            // Hold the window open until *both* transactions have screened.
                            screened.countDown()
                            screened.await(30, TimeUnit.SECONDS)
                            // Step 2 — and what it does second, having decided.
                            if (rejections.isEmpty()) {
                                screenedOk.incrementAndGet()
                                authRoleUserDao.insert(AuthRoleUser {
                                    this.roleId = roleId
                                    this.userId = user
                                })
                                landed.incrementAndGet()
                            }
                        }
                    }.onFailure { println("[SoD window] ${roleId} failed: ${it::class.simpleName}: ${it.message}") }
                }
            }
            pool.shutdown()
            pool.awaitTermination(60, TimeUnit.SECONDS)

            val held = heldRoles()
            val rawRows = committedOffThread {
                authRoleUserDao.searchGrantsByRoleId(maker).count { it.userId == user } +
                    authRoleUserDao.searchGrantsByRoleId(checker).count { it.userId == user }
            }
            println("[SoD window] screening passed for ${screenedOk.get()} of 2; inserts landed=${landed.get()}; raw rows for this user=${rawRows}; resolved roles=${held.size}")
            assertTrue(
                !held.containsAll(setOf(maker, checker)),
                "with both screenings completed before either insert, the principal ended up holding " +
                    "both halves of an exclusive pair. Nothing between the check and the write " +
                    "prevents it: no lock is taken and no database constraint spans the two roles, " +
                    "so separation of duties holds only when the two writes happen not to overlap.",
            )
        } finally {
            cleanUp()
        }
    }

    /**
     * Reads and writes here must happen **off** the test thread.
     *
     * The test method runs inside a transaction that is rolled back at the end, so anything it does
     * itself is invisible to the worker threads and anything they commit is invisible to it. Doing
     * the observation and the cleanup on their own threads is what makes each round see, and leave,
     * genuinely committed state — without it the second round finds rows the first one "deleted",
     * the binds no-op, and the race silently stops being exercised.
     */
    private fun <T> offThread(block: () -> T): T {
        val pool = Executors.newSingleThreadExecutor()
        try {
            return pool.submit<T> { block() }.get(30, TimeUnit.SECONDS)
        } finally {
            pool.shutdown()
        }
    }

    /**
     * Off the test thread **and** inside a committed transaction.
     *
     * Both halves are required and each hides a different trap: off-thread, because the test's own
     * transaction is rolled back and never visible to the workers; in a transaction, because the
     * connection pool runs with auto-commit disabled, so a bare DAO write outside one is silently
     * discarded. A cleanup that quietly does nothing is worse than no cleanup — it lets state leak
     * between rounds and turns the result into an artefact of the harness.
     */
    private fun <T> committedOffThread(block: () -> T): T = offThread {
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }.execute { block() }!!
    }

    private fun heldRoles(): Set<String> =
        committedOffThread { authRoleUserDao.searchRoleIdsByUserId(user).toSet() }

    /** These writes commit for real, so each round has to leave the table as it found it. */
    private fun cleanUp() {
        committedOffThread {
            authRoleUserDao.deleteByRoleIdAndUserId(maker, user)
            authRoleUserDao.deleteByRoleIdAndUserId(checker, user)
        }
    }
}
