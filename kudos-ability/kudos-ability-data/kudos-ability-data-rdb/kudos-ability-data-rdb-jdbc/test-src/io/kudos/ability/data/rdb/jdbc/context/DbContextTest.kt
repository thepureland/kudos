package io.kudos.ability.data.rdb.jdbc.context

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [DbContext] and [DbParam].
 *
 * Covers: create-on-read semantics of get(), getOrNull() not polluting the ThreadLocal,
 * set(null), clear(), child-thread non-inheritance (childValue returns null), and
 * DbParam.copy() snapshot independence.
 *
 * @author K
 * @since 1.0.0
 */
internal class DbContextTest {

    @AfterTest
    fun tearDown() {
        DbContext.clear()
    }

    @Test
    fun getOrNull_returnsNullWhenUnboundAndDoesNotCreate() {
        DbContext.clear()
        assertNull(DbContext.getOrNull(), "getOrNull must not auto-create")
        // still unbound after the peek
        assertNull(DbContext.getOrNull())
    }

    @Test
    fun get_autoCreatesEmptyParamAndWritesItBack() {
        DbContext.clear()
        val created = DbContext.get()
        assertNull(created.forcedDs)
        assertFalse(created.readonly)
        assertFalse(created.enableLog)
        // the same instance is now bound to the thread
        assertSame(created, DbContext.getOrNull())
        assertSame(created, DbContext.get())
    }

    @Test
    fun set_bindsGivenParam_andSetNullClearsBinding() {
        val param = DbParam().apply { forcedDs = "ds1" }
        DbContext.set(param)
        assertSame(param, DbContext.getOrNull())

        DbContext.set(null)
        assertNull(DbContext.getOrNull(), "set(null) nulls the slot")
    }

    @Test
    fun clear_removesBinding() {
        DbContext.set(DbParam().apply { forcedDs = "ds1" })
        DbContext.clear()
        assertNull(DbContext.getOrNull())
    }

    @Test
    fun childThread_doesNotInheritParentParam() {
        DbContext.set(DbParam().apply { forcedDs = "parent-ds" })
        var childValue: DbParam? = DbParam() // sentinel non-null
        val t = Thread {
            childValue = DbContext.getOrNull()
        }
        t.start()
        t.join()
        assertNull(childValue, "child threads must not inherit the parent's DbParam (thread-pool crossover protection)")
        // parent binding untouched
        assertEquals("parent-ds", DbContext.getOrNull()?.forcedDs)
    }

    @Test
    fun dbParam_copy_isIndependentSnapshot() {
        val origin = DbParam().apply {
            forcedDs = "ds-x"
            enableLog = true
            readonly = true
        }
        val copy = origin.copy()
        assertNotSame(origin, copy)
        assertEquals("ds-x", copy.forcedDs)
        assertTrue(copy.enableLog)
        assertTrue(copy.readonly)

        // mutating the copy must not write through to the origin
        copy.forcedDs = "other"
        copy.readonly = false
        assertEquals("ds-x", origin.forcedDs)
        assertTrue(origin.readonly)
    }

    @Test
    fun dbParam_defaults() {
        val p = DbParam()
        assertNull(p.forcedDs)
        assertFalse(p.enableLog)
        assertFalse(p.readonly)
    }
}
