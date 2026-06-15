package io.kudos.ms.user.core.contact.service

import io.kudos.base.query.Criteria
import io.kudos.ms.user.core.contact.dao.UserContactWayDao
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import io.kudos.ms.user.core.contact.service.impl.UserContactWayService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit test for [UserContactWayService.getActiveContactValuesByUserIds] — covers the empty-input
 * short-circuit and the per-user "lowest-priority-number wins, null priority treated as lowest priority"
 * selection logic. The DAO is a Mockito mock; the priority resolution is pure in-memory grouping, so no
 * Spring container / DB is needed (the existing [UserContactWayServiceTest] is Docker-gated and empty).
 *
 * @author K
 * @since 1.0.0
 */
internal class UserContactWayServicePureTest {

    private val dao = mock(UserContactWayDao::class.java)

    private fun build() = UserContactWayService(dao)

    // The service calls dao.search(criteria) with no order varargs, so the orders array is empty;
    // stubbing/verifying with a single Criteria matcher matches that exact zero-vararg invocation.
    /** search(criteria, vararg orders) — register the matcher, return the provided rows. */
    private fun stubSearch(rows: List<UserContactWay>) {
        whenCalled(dao.search(anyCriteria())).thenReturn(rows)
    }

    private fun anyCriteria(): Criteria? = ArgumentMatchers.any(Criteria::class.java)

    private fun row(userId: String, value: String, priority: Int?): UserContactWay {
        val r = mock(UserContactWay::class.java)
        whenCalled(r.userId).thenReturn(userId)
        whenCalled(r.contactWayValue).thenReturn(value)
        whenCalled(r.priority).thenReturn(priority)
        return r
    }

    @Test
    fun emptyUserIds_returnsEmptyWithoutHittingDao() {
        assertTrue(build().getActiveContactValuesByUserIds(emptyList(), "EMAIL").isEmpty())
        verify(dao, never()).search(anyCriteria())
    }

    @Test
    fun picksLowestPriorityNumberPerUser() {
        stubSearch(
            listOf(
                row("u1", "low@x", priority = 10),
                row("u1", "high@x", priority = 1),
                row("u2", "only@x", priority = 5),
            )
        )
        val result = build().getActiveContactValuesByUserIds(listOf("u1", "u2"), "EMAIL")
        assertEquals(mapOf("u1" to "high@x", "u2" to "only@x"), result)
    }

    @Test
    fun nullPriorityTreatedAsLowestPriority_losesToNumberedRow() {
        stubSearch(
            listOf(
                row("u1", "numbered@x", priority = 7),
                row("u1", "nullprio@x", priority = null),
            )
        )
        val result = build().getActiveContactValuesByUserIds(listOf("u1"), "EMAIL")
        // null -> Int.MAX_VALUE, so the numbered (7) row wins.
        assertEquals(mapOf("u1" to "numbered@x"), result)
    }

    @Test
    fun allNullPriority_keepsFirstEncountered() {
        stubSearch(
            listOf(
                row("u1", "first@x", priority = null),
                row("u1", "second@x", priority = null),
            )
        )
        // minByOrNull returns the first element among equal keys.
        assertEquals(mapOf("u1" to "first@x"), build().getActiveContactValuesByUserIds(listOf("u1"), "EMAIL"))
    }

    @Test
    fun noRows_returnsEmptyMap() {
        stubSearch(emptyList())
        assertTrue(build().getActiveContactValuesByUserIds(listOf("u1"), "EMAIL").isEmpty())
    }
}
