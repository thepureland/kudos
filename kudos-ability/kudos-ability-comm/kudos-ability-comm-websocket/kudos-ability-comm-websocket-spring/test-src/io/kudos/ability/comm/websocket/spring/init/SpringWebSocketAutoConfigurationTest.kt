package io.kudos.ability.comm.websocket.spring.init

import io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor
import io.kudos.ability.comm.websocket.common.connect.WebSocketConnectDecision
import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.spring.FakeWebSocketSession
import io.kudos.ability.comm.websocket.spring.handler.InboundOverflowPolicy
import io.kudos.ability.comm.websocket.spring.handler.KudosSpringWebSocketHandler
import io.kudos.ability.comm.websocket.spring.handshake.IWebSocketHandshakeGuard
import io.kudos.ability.comm.websocket.spring.handshake.KudosHandshakeInterceptor
import io.kudos.ability.comm.websocket.spring.handshake.WebSocketHandshakeDecision
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [SpringWebSocketAutoConfiguration]'s endpoint resolution and registration, exercised
 * directly against a recording [WebSocketHandlerRegistry] rather than through a Spring container — the
 * style used by the sibling `WebSocketAutoConfigurationTest`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SpringWebSocketAutoConfigurationTest {

    @Test
    fun endpointBeans_areRegisteredAtTheirOwnPaths() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            endpoints = listOf(
                KudosWebSocketEndpoint("/ws/chat", NoopHandler()),
                KudosWebSocketEndpoint("/ws/notify", NoopHandler()),
            ),
        )

        configuration.registerWebSocketHandlers(registry)

        assertContentEquals(listOf("/ws/chat", "/ws/notify"), registry.paths)
        configuration.destroy()
    }

    @Test
    fun aSingleHandlerBean_fallsBackToTheConfiguredPath() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            handlers = listOf(NoopHandler()),
            properties = SpringWebSocketProperties().apply { path = "/ws/only" },
        )

        configuration.registerWebSocketHandlers(registry)

        assertContentEquals(listOf("/ws/only"), registry.paths)
        configuration.destroy()
    }

    /**
     * Picking one of several handlers arbitrarily would produce an application that starts cleanly and
     * silently serves the wrong thing at the fallback path.
     */
    @Test
    fun severalHandlersAndNoEndpoints_registersNothing() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(handlers = listOf(NoopHandler(), NoopHandler()))

        configuration.registerWebSocketHandlers(registry)

        assertTrue(registry.paths.isEmpty())
        configuration.destroy()
    }

    @Test
    fun noHandlersAtAll_registersNothing() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration()

        configuration.registerWebSocketHandlers(registry)

        assertTrue(registry.paths.isEmpty())
        configuration.destroy()
    }

    @Test
    fun endpointBeans_winOverTheSingleHandlerFallback() {
        val registry = RecordingRegistry()
        val shared = NoopHandler()
        val configuration = newConfiguration(
            endpoints = listOf(KudosWebSocketEndpoint("/ws/explicit", shared)),
            handlers = listOf(shared),
        )

        configuration.registerWebSocketHandlers(registry)

        assertContentEquals(listOf("/ws/explicit"), registry.paths)
        configuration.destroy()
    }

    /** Empty origins must stay empty: that is Spring's same-origin default, and the secure one. */
    @Test
    fun byDefault_noOriginIsConfigured() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(handlers = listOf(NoopHandler()))

        configuration.registerWebSocketHandlers(registry)

        assertTrue(registry.registrations.single().allowedOrigins.isEmpty())
        assertTrue(registry.registrations.single().allowedOriginPatterns.isEmpty())
        configuration.destroy()
    }

    @Test
    fun configuredOrigins_areApplied() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            handlers = listOf(NoopHandler()),
            properties = SpringWebSocketProperties().apply { allowedOrigins = listOf("https://app.example.com") },
        )

        configuration.registerWebSocketHandlers(registry)

        assertContentEquals(listOf("https://app.example.com"), registry.registrations.single().allowedOrigins)
        configuration.destroy()
    }

    /** Patterns are the fallback, so tightening a deployment means setting one key rather than clearing another. */
    @Test
    fun originPatterns_applyOnlyWhenExactOriginsAreEmpty() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            handlers = listOf(NoopHandler()),
            properties = SpringWebSocketProperties().apply {
                allowedOrigins = listOf("https://app.example.com")
                allowedOriginPatterns = listOf("https://ignored.example.com")
            },
        )

        configuration.registerWebSocketHandlers(registry)

        assertContentEquals(listOf("https://app.example.com"), registry.registrations.single().allowedOrigins)
        assertTrue(registry.registrations.single().allowedOriginPatterns.isEmpty())
        configuration.destroy()
    }

    @Test
    fun anEndpointCanOverrideTheModuleWideOrigins() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            endpoints = listOf(KudosWebSocketEndpoint("/ws/a", NoopHandler(), allowedOrigins = listOf("https://a.example.com"))),
            properties = SpringWebSocketProperties().apply { allowedOrigins = listOf("https://global.example.com") },
        )

        configuration.registerWebSocketHandlers(registry)

        assertContentEquals(listOf("https://a.example.com"), registry.registrations.single().allowedOrigins)
        configuration.destroy()
    }

    @Test
    fun sockJs_isOffByDefaultAndOptInPerEndpoint() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            endpoints = listOf(
                KudosWebSocketEndpoint("/ws/plain", NoopHandler()),
                KudosWebSocketEndpoint("/ws/legacy", NoopHandler(), sockJs = true),
            ),
        )

        configuration.registerWebSocketHandlers(registry)

        assertEquals(false, registry.registrations[0].sockJs)
        assertEquals(true, registry.registrations[1].sockJs)
        configuration.destroy()
    }

    @Test
    fun everyEndpointGetsTheKudosHandshakeInterceptor() {
        val registry = RecordingRegistry()
        val configuration = newConfiguration(endpoints = listOf(KudosWebSocketEndpoint("/ws/a", NoopHandler())))

        configuration.registerWebSocketHandlers(registry)

        assertTrue(registry.registrations.single().interceptors.any {
            it is io.kudos.ability.comm.websocket.spring.handshake.KudosHandshakeInterceptor
        })
        configuration.destroy()
    }

    @Test
    fun containerFactory_carriesTheConfiguredBufferSizes() {
        val configuration = newConfiguration(
            properties = SpringWebSocketProperties().apply {
                container.maxTextMessageBufferSize = 128 * 1024
                container.maxBinaryMessageBufferSize = 256 * 1024
            },
        )

        val factory = configuration.servletServerContainerFactoryBean()

        // Bound properties that never reach the object they configure are a bug the container starts happily with.
        assertEquals(128 * 1024, factory.maxTextMessageBufferSize)
        assertEquals(256 * 1024, factory.maxBinaryMessageBufferSize)
    }

    /** `0` means "leave the container's own default alone", not "time out immediately". */
    @Test
    fun containerFactory_leavesZeroTimeoutsUnset() {
        val factory = newConfiguration().servletServerContainerFactoryBean()

        assertEquals(null, factory.maxSessionIdleTimeout)
        assertEquals(null, factory.asyncSendTimeout)
    }

    /**
     * `ifEmpty { global }` is trivial to write backwards, and getting it backwards silently drops an
     * endpoint's own quota rules in favour of the module-wide ones — or vice versa. Observed through
     * the real handler the configuration built, since that is the only thing the wiring produces.
     */
    @Test
    fun endpointInterceptors_replaceTheModuleWideOnes() {
        val ran = mutableListOf<String>()
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            endpoints = listOf(
                KudosWebSocketEndpoint("/ws/a", NoopHandler(),
                    connectInterceptors = listOf(IWebSocketConnectInterceptor { ran += "endpoint"; WebSocketConnectDecision.Proceed })),
            ),
            interceptors = listOf(IWebSocketConnectInterceptor { ran += "global"; WebSocketConnectDecision.Proceed }),
        )

        configuration.registerWebSocketHandlers(registry)
        driveOneConnection(registry) { ran.isNotEmpty() }

        assertContentEquals(listOf("endpoint"), ran)
        configuration.destroy()
    }

    @Test
    fun moduleWideInterceptors_applyWhenAnEndpointDeclaresNone() {
        val ran = mutableListOf<String>()
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            endpoints = listOf(KudosWebSocketEndpoint("/ws/a", NoopHandler())),
            interceptors = listOf(IWebSocketConnectInterceptor { ran += "global"; WebSocketConnectDecision.Proceed }),
        )

        configuration.registerWebSocketHandlers(registry)
        driveOneConnection(registry) { ran.isNotEmpty() }

        assertContentEquals(listOf("global"), ran)
        configuration.destroy()
    }

    @Test
    fun endpointGuards_replaceTheModuleWideOnes() {
        val ran = mutableListOf<String>()
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            endpoints = listOf(
                KudosWebSocketEndpoint("/ws/a", NoopHandler(),
                    handshakeGuards = listOf(IWebSocketHandshakeGuard { _, _ -> ran += "endpoint"; WebSocketHandshakeDecision.Proceed })),
            ),
            guards = listOf(IWebSocketHandshakeGuard { _, _ -> ran += "global"; WebSocketHandshakeDecision.Proceed }),
        )

        configuration.registerWebSocketHandlers(registry)
        driveHandshake(registry)

        assertContentEquals(listOf("endpoint"), ran)
        configuration.destroy()
    }

    @Test
    fun moduleWideGuards_applyWhenAnEndpointDeclaresNone() {
        val ran = mutableListOf<String>()
        val registry = RecordingRegistry()
        val configuration = newConfiguration(
            endpoints = listOf(KudosWebSocketEndpoint("/ws/a", NoopHandler())),
            guards = listOf(IWebSocketHandshakeGuard { _, _ -> ran += "global"; WebSocketHandshakeDecision.Proceed }),
        )

        configuration.registerWebSocketHandlers(registry)
        driveHandshake(registry)

        assertContentEquals(listOf("global"), ran)
        configuration.destroy()
    }

    /**
     * Same class of bug the container-buffer test guards, for the limits that matter under load: a
     * queue capacity or send ceiling that is bound from yaml but never reaches the handler is a limit
     * that only appears to exist.
     */
    @Test
    fun queueAndSendProperties_reachTheHandlerOptions() {
        val configuration = newConfiguration(
            properties = SpringWebSocketProperties().apply {
                inbound.bufferCapacity = 7
                inbound.overflow = InboundOverflowPolicy.CLOSE
                send.timeLimitMillis = 1234
                send.bufferSizeLimitBytes = 4321
                send.overflowStrategy = ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP
                shutdownTimeoutMillis = 999
            },
        )

        val options = configuration.options()

        assertEquals(7, options.inboundBufferCapacity)
        assertEquals(InboundOverflowPolicy.CLOSE, options.inboundOverflow)
        assertEquals(1234, options.sendTimeLimitMillis)
        assertEquals(4321, options.sendBufferSizeLimitBytes)
        assertEquals(ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP, options.sendOverflowStrategy)
        assertEquals(999, options.shutdownTimeoutMillis)
    }

    @Test
    fun defaultOptions_matchThePropertyDefaults() {
        val options = newConfiguration().options()

        assertEquals(64, options.inboundBufferCapacity)
        assertEquals(InboundOverflowPolicy.BACKPRESSURE, options.inboundOverflow,
            "Not losing messages is the right default; shedding load has to be chosen deliberately")
        assertEquals(512 * 1024, options.sendBufferSizeLimitBytes)
        assertEquals(ConcurrentWebSocketSessionDecorator.OverflowStrategy.TERMINATE, options.sendOverflowStrategy)
    }

    @Test
    fun componentName_identifiesThisModule() {
        assertEquals("kudos-ability-comm-websocket-spring", newConfiguration().getComponentName())
    }

    private fun newConfiguration(
        endpoints: List<IKudosWebSocketEndpoint> = emptyList(),
        handlers: List<IKudosWebSocketHandler> = emptyList(),
        guards: List<IWebSocketHandshakeGuard> = emptyList(),
        interceptors: List<IWebSocketConnectInterceptor> = emptyList(),
        properties: SpringWebSocketProperties = SpringWebSocketProperties(),
    ) = SpringWebSocketAutoConfiguration(
        properties = properties,
        sessionRegistry = KudosWebSocketRegistry(),
        endpointProvider = ListProvider(endpoints),
        handlerProvider = ListProvider(handlers),
        guardProvider = ListProvider(guards),
        interceptorProvider = ListProvider(interceptors),
    )

    /**
     * Opens one connection on the handler the configuration registered, so admission wiring can be
     * observed through the object that actually runs rather than through a getter added for the test.
     */
    private fun driveOneConnection(registry: RecordingRegistry, until: () -> Boolean) {
        val handler = registry.registrations.single().handler as KudosSpringWebSocketHandler
        val raw = FakeWebSocketSession()
        handler.afterConnectionEstablished(raw)
        // Admission runs on the pump coroutine, so there is no callback to await — poll for it.
        val deadlineNanos = System.nanoTime() + 5_000L * 1_000_000
        while (System.nanoTime() < deadlineNanos && !until()) Thread.sleep(5)
        handler.afterConnectionClosed(raw, CloseStatus.NORMAL)
        handler.destroy()
        assertTrue(until(), "The connection never reached admission control")
    }

    /** Runs the registered [KudosHandshakeInterceptor] so guard wiring can be observed the same way. */
    private fun driveHandshake(registry: RecordingRegistry) {
        val interceptor = registry.registrations.single().interceptors
            .filterIsInstance<KudosHandshakeInterceptor>().single()
        val request = ServletServerHttpRequest(MockHttpServletRequest("GET", "/ws/a"))
        val response = ServletServerHttpResponse(MockHttpServletResponse())
        interceptor.beforeHandshake(request, response, TextWebSocketHandler(), mutableMapOf())
    }

    private class NoopHandler : IKudosWebSocketHandler

    /** Captures what the configuration asked the registry for. */
    private class RecordingRegistry : WebSocketHandlerRegistry {
        val registrations = mutableListOf<RecordingRegistration>()
        val paths: List<String> get() = registrations.map { it.path }

        override fun addHandler(handler: WebSocketHandler, vararg paths: String): WebSocketHandlerRegistration =
            RecordingRegistration(paths.single(), handler).also { registrations += it }
    }

    private class RecordingRegistration(val path: String, val handler: WebSocketHandler) : WebSocketHandlerRegistration {
        val interceptors = mutableListOf<HandshakeInterceptor>()
        var allowedOrigins: List<String> = emptyList()
        var allowedOriginPatterns: List<String> = emptyList()
        var sockJs = false

        override fun addInterceptors(vararg interceptors: HandshakeInterceptor) = apply {
            this.interceptors += interceptors
        }

        override fun setAllowedOrigins(vararg origins: String) = apply { allowedOrigins = origins.toList() }

        override fun setAllowedOriginPatterns(vararg patterns: String) = apply { allowedOriginPatterns = patterns.toList() }

        override fun addHandler(handler: WebSocketHandler, vararg paths: String) = this

        override fun setHandshakeHandler(handshakeHandler: HandshakeHandler) = this

        override fun withSockJS(): SockJsServiceRegistration {
            sockJs = true
            return SockJsServiceRegistration()
        }
    }

    /** Minimal [ObjectProvider] over a fixed list; only the streaming accessors are used here. */
    private class ListProvider<T : Any>(private val items: List<T>) : ObjectProvider<T> {
        override fun getObject(vararg args: Any?): T = items.first()

        override fun getObject(): T = items.first()

        override fun getIfAvailable(): T? = items.firstOrNull()

        override fun getIfUnique(): T? = items.singleOrNull()

        override fun stream(): Stream<T> = items.stream()

        override fun orderedStream(): Stream<T> = items.stream()
    }
}
