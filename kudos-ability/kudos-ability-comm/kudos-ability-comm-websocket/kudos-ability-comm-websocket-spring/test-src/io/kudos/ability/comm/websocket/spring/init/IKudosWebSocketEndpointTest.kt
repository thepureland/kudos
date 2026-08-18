package io.kudos.ability.comm.websocket.spring.init

import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.spring.FakeWebSocketSession
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the defaults on [IKudosWebSocketEndpoint] / [KudosWebSocketEndpoint].
 *
 * The nullable overrides are the subtle part: `null` means "inherit the module-wide setting" while an
 * **empty** list or an explicit `false` means "this endpoint really wants none". Collapsing the two —
 * the obvious simplification — would make it impossible to declare a public endpoint that opts out of a
 * module-wide origin restriction, and `SpringWebSocketAutoConfiguration` reads them expecting the
 * distinction to hold.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class IKudosWebSocketEndpointTest {

    private val handler = object : IKudosWebSocketHandler {}

    @Test
    fun theDefaultSessionFactory_producesAnAnonymousSession(): Unit = runBlocking {
        val endpoint = KudosWebSocketEndpoint("/ws", handler)

        val session = assertNotNull(endpoint.sessionFactory.create(FakeWebSocketSession(sessionId = "s1")))

        assertEquals("s1", session.sessionId)
        // Anonymous means reachable by broadcast() and unicast() only — never by broadcastToUser /
        // broadcastToTenant, since it is in neither index.
        assertNull(session.userId)
        assertNull(session.tenantId)
    }

    @Test
    fun theDefaultSessionFactory_neverRejects(): Unit = runBlocking {
        val endpoint = KudosWebSocketEndpoint("/ws", handler)

        assertNotNull(endpoint.sessionFactory.create(FakeWebSocketSession()),
            "An endpoint that declares no identification must still accept connections")
    }

    @Test
    fun collectionDefaults_areEmptyMeaningInheritTheModuleWideBeans() {
        val endpoint = KudosWebSocketEndpoint("/ws", handler)

        assertTrue(endpoint.connectInterceptors.isEmpty())
        assertTrue(endpoint.handshakeGuards.isEmpty())
    }

    /** `null`, not `emptyList()`: an empty origin list is itself meaningful, so it cannot double as "unset". */
    @Test
    fun nullableOverrides_defaultToNullRatherThanEmpty() {
        val endpoint = KudosWebSocketEndpoint("/ws", handler)

        assertNull(endpoint.allowedOrigins)
        assertNull(endpoint.sockJs)
    }

    @Test
    fun anEmptyOriginList_isDistinguishableFromUnset() {
        val unset = KudosWebSocketEndpoint("/ws", handler)
        val explicitlyEmpty = KudosWebSocketEndpoint("/ws", handler, allowedOrigins = emptyList())

        assertNull(unset.allowedOrigins)
        assertEquals(emptyList(), explicitlyEmpty.allowedOrigins,
            "An endpoint must be able to say 'same-origin only' even when the module allows more")
    }

    @Test
    fun anInterfaceImplementationInheritsTheSameDefaults(): Unit = runBlocking {
        val endpoint = object : IKudosWebSocketEndpoint {
            override val path = "/ws/custom"
            override val handler = this@IKudosWebSocketEndpointTest.handler
        }

        assertNotNull(endpoint.sessionFactory.create(FakeWebSocketSession()))
        assertTrue(endpoint.connectInterceptors.isEmpty())
        assertTrue(endpoint.handshakeGuards.isEmpty())
        assertNull(endpoint.allowedOrigins)
        assertNull(endpoint.sockJs)
    }
}
