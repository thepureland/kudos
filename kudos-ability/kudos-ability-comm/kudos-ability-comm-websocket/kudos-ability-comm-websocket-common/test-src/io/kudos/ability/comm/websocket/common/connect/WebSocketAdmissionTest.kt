package io.kudos.ability.comm.websocket.common.connect

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Unit tests for [admit], the admission chain shared by every engine adapter.
 *
 * Both engines already exercise this indirectly (`KudosWebSocketRoutingTest`,
 * `KudosSpringWebSocketHandlerTest`), so why test it directly as well: **the fail-closed rule is a
 * security property**, and it was extracted here precisely so there is one copy of it rather than one
 * per engine. A property that only has indirect coverage is a property whose regression shows up as a
 * confusing engine-level test failure — or, if only one engine's test happens to cover the branch, not
 * at all.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class WebSocketAdmissionTest {

    private val session = StubSessionRef("s1", userId = "u1", tenantId = "t1")

    @Test
    fun noInterceptors_admits(): Unit = runBlocking {
        assertNull(emptyList<IWebSocketConnectInterceptor>().admit(session))
    }

    @Test
    fun allProceeding_admits_andEveryInterceptorRuns(): Unit = runBlocking {
        val ran = mutableListOf<String>()
        val interceptors = (1..3).map { n ->
            IWebSocketConnectInterceptor { ran += "i$n"; WebSocketConnectDecision.Proceed }
        }

        assertNull(interceptors.admit(session))
        assertEquals(listOf("i1", "i2", "i3"), ran)
    }

    @Test
    fun theFirstRejectionWins_andLaterInterceptorsDoNotRun(): Unit = runBlocking {
        val ran = mutableListOf<String>()
        val interceptors = listOf(
            IWebSocketConnectInterceptor { ran += "first"; WebSocketConnectDecision.Proceed },
            IWebSocketConnectInterceptor {
                ran += "second"
                WebSocketConnectDecision.Reject(WebSocketCloseReason.Codes.TRY_AGAIN_LATER, "full")
            },
            IWebSocketConnectInterceptor { ran += "third"; WebSocketConnectDecision.Proceed },
        )

        val rejection = interceptors.admit(session)

        assertEquals(WebSocketCloseReason.Codes.TRY_AGAIN_LATER.code, rejection?.reason?.code)
        assertEquals("full", rejection?.reason?.message)
        assertEquals(listOf("first", "second"), ran, "A rejected connection must not keep paying for further checks")
    }

    /**
     * The property this whole file exists for: a quota or ban check that blew up must never be read as
     * "quota available". Fail **closed**.
     */
    @Test
    fun aThrowingInterceptor_isTreatedAsARejection(): Unit = runBlocking {
        val interceptors = listOf(
            IWebSocketConnectInterceptor { error("quota service unreachable") },
        )

        val rejection = interceptors.admit(session)

        assertIs<WebSocketConnectDecision.Reject>(rejection)
        assertEquals(WebSocketCloseReason.Codes.INTERNAL_ERROR.code, rejection.reason.code)
    }

    @Test
    fun aThrowingInterceptor_stopsTheChainLikeAnyOtherRejection(): Unit = runBlocking {
        val ran = mutableListOf<String>()
        val interceptors = listOf(
            IWebSocketConnectInterceptor { ran += "boom"; error("down") },
            IWebSocketConnectInterceptor { ran += "later"; WebSocketConnectDecision.Proceed },
        )

        interceptors.admit(session)

        assertEquals(listOf("boom"), ran)
    }

    /**
     * Cancellation is a shutdown signal, not an interceptor failure. Swallowing it into a `Reject`
     * would make a cancelled connection look like a policy decision and leave the coroutine believing
     * it may continue.
     */
    @Test
    fun cancellation_isRethrownRatherThanConvertedIntoARejection() {
        val interceptors = listOf(
            IWebSocketConnectInterceptor { throw CancellationException("shutting down") },
        )

        assertFailsWith<CancellationException> {
            runBlocking { interceptors.admit(session) }
        }
    }

    @Test
    fun theSessionIsHandedToEveryInterceptor(): Unit = runBlocking {
        val seen = mutableListOf<String?>()
        val interceptors = (1..2).map {
            IWebSocketConnectInterceptor { s -> seen += s.userId; WebSocketConnectDecision.Proceed }
        }

        interceptors.admit(session)

        assertEquals(listOf<String?>("u1", "u1"), seen.toList())
    }

    /** Plain stub: admission only reads metadata, so no engine is involved. */
    private class StubSessionRef(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) = Unit
        override suspend fun sendBinary(bytes: ByteArray) = Unit
        override suspend fun close(reason: WebSocketCloseReason) = Unit
    }
}
