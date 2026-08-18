package io.kudos.ability.comm.websocket.common.connect

import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketSessionRef
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for [MaxConnectionsInterceptor] — plain stub sessions, no Ktor engine (which is the
 * point of typing the interceptor SPI against `KudosWebSocketSessionRef`).
 *
 * Covers each dimension (per user / per tenant / per node), the "0 disables" rule, anonymous
 * sessions skipping identity dimensions, and the close code used for rejection.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class MaxConnectionsInterceptorTest {

    @Test
    fun underThePerUserLimit_proceeds() = runBlocking<Unit> {
        val registry = KudosWebSocketRegistry()
        registry.register(StubSession("existing", userId = "u1"))
        val interceptor = MaxConnectionsInterceptor(registry, maxPerUser = 2)

        assertIs<WebSocketConnectDecision.Proceed>(interceptor.intercept(StubSession("new", userId = "u1")))
    }

    @Test
    fun atThePerUserLimit_rejectsWithTryAgainLater() = runBlocking<Unit> {
        val registry = KudosWebSocketRegistry()
        repeat(2) { registry.register(StubSession("existing-$it", userId = "u1")) }
        val interceptor = MaxConnectionsInterceptor(registry, maxPerUser = 2)

        val decision = interceptor.intercept(StubSession("new", userId = "u1"))

        val reject = assertIs<WebSocketConnectDecision.Reject>(decision)
        assertEquals(WebSocketCloseReason.Codes.TRY_AGAIN_LATER.code, reject.reason.code,
            "The client did nothing wrong; a retry once other connections drain is the right behavior")
    }

    @Test
    fun anotherUsersConnections_doNotCountAgainstYou() = runBlocking<Unit> {
        val registry = KudosWebSocketRegistry()
        repeat(5) { registry.register(StubSession("other-$it", userId = "u2")) }
        val interceptor = MaxConnectionsInterceptor(registry, maxPerUser = 2)

        assertIs<WebSocketConnectDecision.Proceed>(interceptor.intercept(StubSession("new", userId = "u1")))
    }

    @Test
    fun perTenantLimit_isEnforced() = runBlocking<Unit> {
        val registry = KudosWebSocketRegistry()
        repeat(3) { registry.register(StubSession("t-$it", userId = "u$it", tenantId = "acme")) }
        val interceptor = MaxConnectionsInterceptor(registry, maxPerUser = 0, maxPerTenant = 3)

        assertIs<WebSocketConnectDecision.Reject>(interceptor.intercept(StubSession("new", userId = "u9", tenantId = "acme")))
        assertIs<WebSocketConnectDecision.Proceed>(interceptor.intercept(StubSession("new", userId = "u9", tenantId = "other")))
    }

    @Test
    fun nodeWideLimit_isEnforcedRegardlessOfIdentity() = runBlocking<Unit> {
        val registry = KudosWebSocketRegistry()
        repeat(4) { registry.register(StubSession("s-$it")) }
        val interceptor = MaxConnectionsInterceptor(registry, maxPerUser = 0, maxTotal = 4)

        assertIs<WebSocketConnectDecision.Reject>(interceptor.intercept(StubSession("new", userId = "brand-new")))
    }

    @Test
    fun zeroLimits_disableEveryDimension() = runBlocking<Unit> {
        val registry = KudosWebSocketRegistry()
        repeat(50) { registry.register(StubSession("s-$it", userId = "u1", tenantId = "acme")) }
        val interceptor = MaxConnectionsInterceptor(registry, maxPerUser = 0, maxPerTenant = 0, maxTotal = 0)

        assertIs<WebSocketConnectDecision.Proceed>(interceptor.intercept(StubSession("new", userId = "u1", tenantId = "acme")))
    }

    @Test
    fun anonymousSessions_skipTheIdentityDimensions() = runBlocking<Unit> {
        val registry = KudosWebSocketRegistry()
        repeat(10) { registry.register(StubSession("anon-$it")) } // no userId / tenantId
        val interceptor = MaxConnectionsInterceptor(registry, maxPerUser = 1, maxPerTenant = 1)

        // There is no identity to count these against; only maxTotal could bound them.
        assertIs<WebSocketConnectDecision.Proceed>(interceptor.intercept(StubSession("new")))
    }

    private class StubSession(
        override val sessionId: String,
        override val userId: String? = null,
        override val tenantId: String? = null,
    ) : KudosWebSocketSessionRef {
        override val attributes: MutableMap<String, Any> = ConcurrentHashMap()
        override suspend fun sendText(text: String) {}
        override suspend fun sendBinary(bytes: ByteArray) {}
        override suspend fun close(reason: WebSocketCloseReason) {}
    }
}

