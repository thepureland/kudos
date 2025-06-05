package io.kudos.base.support

import kotlin.test.Test
import kotlin.test.assertEquals


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

}