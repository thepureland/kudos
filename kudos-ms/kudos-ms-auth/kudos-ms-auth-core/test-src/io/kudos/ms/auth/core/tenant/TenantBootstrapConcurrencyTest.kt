package io.kudos.ms.auth.core.tenant

import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.tenant.service.iservice.ITenantBootstrapService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Is "first administrator" actually a one-time act when two appointments race?
 *
 * Test data source: `TenantBootstrapConcurrencyTest.sql`
 *
 * **The window under test.** `bindFirstAdministrator` reads the role's holder set, refuses if it is
 * non-empty and `force` was not given, then inserts. The gate's invariant is a property of the
 * **role** — "nobody holds this yet" — while the only locks on the write path were per-principal:
 * two concurrent appointments naming two *different* candidates lock two different principal rows,
 * both read an empty holder set, and both pass. The endpoint's own comment calls a quietly appointed
 * second administrator an escalation path; the race did exactly that, no `force` required.
 *
 * Closed by locking the role row (see `AuthRoleDao.lockRole`) before the holder set is read.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class TenantBootstrapConcurrencyTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: ITenantBootstrapService

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var transactionManager: PlatformTransactionManager

    private val tenant = "tbr-tenant-7q4c"
    private val candidateA = "a1710000-0000-0000-0000-000000000001"
    private val candidateB = "a1710000-0000-0000-0000-000000000002"

    /** Sequential control: the gate itself works when the two appointments do not overlap. */
    @Test
    fun sequentially_theSecondAppointmentIsRefusedWithoutForce() {
        val roleId = seededRoleId()
        try {
            committedOffThread { service.bindFirstAdministrator(tenant, candidateA, null, force = false) }
            val refused = offThread {
                runCatching { service.bindFirstAdministrator(tenant, candidateB, null, force = false) }.isFailure
            }
            assertTrue(refused, "a second appointment without force must be refused")
            assertEquals(1, holders(roleId).size)
        } finally {
            cleanUp(roleId)
        }
    }

    /**
     * The race: both appointments released at the same instant, repeatedly. However they interleave,
     * exactly one may succeed — the other must find the holder set non-empty and refuse.
     */
    @Test
    fun concurrently_onlyOneFirstAdministratorMayLand() {
        val roleId = seededRoleId()
        val attempts = 20
        var breaches = 0

        repeat(attempts) {
            try {
                val start = CountDownLatch(1)
                val done = CountDownLatch(2)
                val successes = AtomicInteger()
                val pool = Executors.newFixedThreadPool(2)
                listOf(candidateA, candidateB).forEach { candidate ->
                    pool.submit {
                        start.await()
                        runCatching { service.bindFirstAdministrator(tenant, candidate, null, force = false) }
                            .onSuccess { successes.incrementAndGet() }
                        done.countDown()
                    }
                }
                start.countDown()
                done.await(30, TimeUnit.SECONDS)
                pool.shutdown()

                if (holders(roleId).size > 1) breaches++
            } finally {
                cleanUp(roleId)
            }
        }

        println("[first-admin race] ${breaches}/${attempts} rounds ended with more than one administrator")
        assertTrue(
            breaches == 0,
            "in ${breaches}/${attempts} rounds the tenant ended up with two first administrators, " +
                "neither appointed with force: both appointments read the holder set as empty " +
                "because nothing serialises them on the role.",
        )
    }

    // -------------------- harness --------------------
    // Same conventions as SoDConcurrencyTest: observation and cleanup run off-thread in committed
    // transactions, because the test's own transaction is rolled back and the pool has auto-commit
    // off.

    /** Seeds the bootstrap roles once (idempotent, committed) and returns the primary role's id. */
    private fun seededRoleId(): String = committedOffThread {
        service.seedRoles(tenant)
        requireNotNull(authRoleDao.searchRoleByTenantIdAndRoleCode(tenant, "tenant-admin")).id
    }

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

    private fun holders(roleId: String): Set<String> =
        committedOffThread { authRoleUserDao.searchUserIdsByRoleId(roleId).toSet() }

    private fun cleanUp(roleId: String) {
        committedOffThread {
            listOf(candidateA, candidateB).forEach { authRoleUserDao.deleteByRoleIdAndUserId(roleId, it) }
        }
    }
}
