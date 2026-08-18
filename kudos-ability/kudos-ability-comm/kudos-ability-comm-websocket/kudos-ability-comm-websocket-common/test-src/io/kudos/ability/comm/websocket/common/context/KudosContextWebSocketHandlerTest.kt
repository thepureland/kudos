package io.kudos.ability.comm.websocket.common.context

import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.core.currentKudosContext
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [KudosContextWebSocketHandler] and [KudosContextThreadElement].
 *
 * The load-bearing case is [threadLocal_survivesASuspensionPointAndAThreadHop]: a hand-written
 * `KudosContextHolder.set(...)` / `finally { clear() }` pair around the delegate would pass every
 * other test here and fail that one, because a coroutine can resume on a different worker thread.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KudosContextWebSocketHandlerTest {

    @AfterTest
    fun tearDown() {
        // These tests deliberately inspect a ThreadLocal; leaving one bound would leak into siblings
        // running on the same JUnit worker thread.
        KudosContextHolder.clear()
    }

    private fun session(userId: String? = "u1", tenantId: String? = "acme") =
        StubSession("s1", userId, tenantId)

    @Test
    fun tenantIdFromTheSession_isVisibleThroughTheThreadLocalHolder() = runBlocking<Unit> {
        var seen: String? = null
        val handler = KudosContextWebSocketHandler(object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                // What every ability-layer consumer actually reads.
                seen = KudosContextHolder.getOrNull()?.tenantId
            }
        })

        handler.onText(session(tenantId = "acme"), "hi")

        assertEquals("acme", seen, "The session's tenant must reach ThreadLocal-based consumers")
    }

    @Test
    fun contextIsAlsoVisibleThroughTheCoroutineElement() = runBlocking<Unit> {
        var seen: String? = null
        val handler = KudosContextWebSocketHandler(object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                seen = currentKudosContext().tenantId
            }
        })

        handler.onText(session(tenantId = "acme"), "hi")

        assertEquals("acme", seen, "currentKudosContext() must work too — half a bridge is a trap")
    }

    @Test
    fun threadLocal_survivesASuspensionPointAndAThreadHop() = runBlocking<Unit> {
        var beforeHop: String? = null
        var onOtherThread: String? = null
        var afterHop: String? = null
        var startThread: Thread? = null
        var hopThread: Thread? = null

        val handler = KudosContextWebSocketHandler(object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                startThread = Thread.currentThread()
                beforeHop = KudosContextHolder.getOrNull()?.tenantId

                // Run part of the message on another dispatcher's worker. This is the case a
                // hand-written set/finally-clear cannot handle: that worker never had the set() run
                // on it, so it would see nothing here.
                withContext(Dispatchers.Default) {
                    delay(20)
                    hopThread = Thread.currentThread()
                    onOtherThread = KudosContextHolder.getOrNull()?.tenantId
                }

                afterHop = KudosContextHolder.getOrNull()?.tenantId
            }
        })

        handler.onText(session(tenantId = "acme"), "hi")

        assertEquals("acme", beforeHop)
        assertNotEquals(startThread, hopThread, "The test is meaningless unless the work really moved threads")
        assertEquals("acme", onOtherThread,
            "The context must follow the coroutine onto whatever thread it resumes on")
        assertEquals("acme", afterHop, "...and still be there once it comes back")
        assertNull(KudosContextHolder.getOrNull(), "The borrowed worker threads must be left clean")
    }

    @Test
    fun threadLocalIsCleared_afterTheHookReturns() = runBlocking<Unit> {
        val handler = KudosContextWebSocketHandler(object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                KudosContextHolder.getOrNull() // touch it; must not survive below
            }
        })

        handler.onText(session(), "hi")

        assertNull(KudosContextHolder.getOrNull(),
            "Nothing may be left bound to a pooled thread once the message is handled")
    }

    @Test
    fun aPreExistingContextOnTheThread_isRestoredNotClobbered() = runBlocking<Unit> {
        val outer = KudosContext().apply { tenantId = "outer-tenant" }
        KudosContextHolder.set(outer)
        var innerTenant: String? = null

        val handler = KudosContextWebSocketHandler(object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                innerTenant = KudosContextHolder.getOrNull()?.tenantId
            }
        })

        handler.onText(session(tenantId = "inner-tenant"), "hi")

        assertEquals("inner-tenant", innerTenant)
        assertSame(outer, KudosContextHolder.getOrNull(),
            "The thread's previous context must be put back, not dropped")
    }

    @Test
    fun everyHook_isWrapped() = runBlocking<Unit> {
        val seen = mutableListOf<String?>()
        val recorder = object : IKudosWebSocketHandler {
            override suspend fun onConnect(session: KudosWebSocketSessionRef) { seen += tenant() }
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) { seen += tenant() }
            override suspend fun onBinary(session: KudosWebSocketSessionRef, bytes: ByteArray) { seen += tenant() }
            override suspend fun onDisconnect(session: KudosWebSocketSessionRef, cause: Throwable?) { seen += tenant() }
            private fun tenant() = KudosContextHolder.getOrNull()?.tenantId
        }
        val handler = KudosContextWebSocketHandler(recorder)
        val s = session(tenantId = "acme")

        handler.onConnect(s)
        handler.onText(s, "t")
        handler.onBinary(s, byteArrayOf(1))
        handler.onDisconnect(s, null)

        assertEquals(listOf<String?>("acme", "acme", "acme", "acme"), seen.toList(),
            "onDisconnect included — business cleanup often needs the tenant to reach the database")
    }

    @Test
    fun eachInvocationGetsItsOwnContextAndTraceKey() = runBlocking<Unit> {
        val traceKeys = mutableListOf<String?>()
        val handler = KudosContextWebSocketHandler(object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                traceKeys += KudosContextHolder.getOrNull()?.traceKey
            }
        })
        val s = session()

        handler.onText(s, "first")
        handler.onText(s, "second")

        assertEquals(2, traceKeys.size)
        assertTrue(traceKeys.all { !it.isNullOrBlank() }, "Every message should be traceable")
        assertNotEquals(traceKeys[0], traceKeys[1],
            "A per-connection traceKey would make every message in a long-lived session share one id")
    }

    @Test
    fun customFactory_isUsed() = runBlocking<Unit> {
        var seenPortal: String? = null
        val handler = KudosContextWebSocketHandler(
            object : IKudosWebSocketHandler {
                override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
                    seenPortal = KudosContextHolder.getOrNull()?.portalCode
                }
            },
            contextFactory = { s -> KudosContext().apply { portalCode = "portal-of-${s.sessionId}" } },
        )

        handler.onText(session(), "hi")

        assertEquals("portal-of-s1", seenPortal, "The factory is the seam for what this module cannot fill in")
    }

    @Test
    fun contextIsUnboundEvenWhenTheDelegateThrows() = runBlocking<Unit> {
        val handler = KudosContextWebSocketHandler(object : IKudosWebSocketHandler {
            override suspend fun onText(session: KudosWebSocketSessionRef, text: String): Unit =
                throw IllegalStateException("business blew up")
        })

        runCatching { handler.onText(session(), "hi") }

        assertNull(KudosContextHolder.getOrNull(), "An exception must not leave a context bound to the thread")
    }

    private class StubSession(
        override val sessionId: String,
        override val userId: String?,
        override val tenantId: String?,
    ) : KudosWebSocketSessionRef {
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {}
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: WebSocketCloseReason) {}
    }
}
