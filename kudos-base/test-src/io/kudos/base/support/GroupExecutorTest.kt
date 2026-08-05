package io.kudos.base.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * test for GroupExecutor
 *
 * @author K
 * @since 1.0.0
 */
internal class GroupExecutorTest {

    @Test
    fun testExecute() {
        val elems = IntRange(1, 50).toList()
        val sb = StringBuilder()
        GroupExecutor(elems, 10) {
            if (it.isNotEmpty()) {
                sb.append(it[0]).append(",")
            }
        }.execute()
        assertEquals("1,11,21,31,41,", sb.toString())
    }

    @Test
    fun testExecuteWithDefaultGroupSize() {
        // omitting groupSize uses the default (1000) constructor path; small collection -> single group
        val elems = (1..5).toList()
        val seen = mutableListOf<List<Int>>()
        GroupExecutor(elems) { seen.add(it.toList()) }.execute()
        assertEquals(1, seen.size, "small collection fits in one default-sized group")
        assertEquals(elems, seen[0])
    }

    @Test
    fun testExecuteWithEmptyCollectionRunsNothing() {
        var invoked = false
        GroupExecutor(emptyList<Int>(), 10) { invoked = true }.execute()
        assertTrue(!invoked, "empty collection should not invoke the operation")
    }

    @Test
    fun testLastGroupSmallerThanGroupSize() {
        val elems = (1..7).toList()
        val sizes = mutableListOf<Int>()
        GroupExecutor(elems, 3) { sizes.add(it.size) }.execute()
        assertEquals(listOf(3, 3, 1), sizes, "last group holds the remainder")
    }
}