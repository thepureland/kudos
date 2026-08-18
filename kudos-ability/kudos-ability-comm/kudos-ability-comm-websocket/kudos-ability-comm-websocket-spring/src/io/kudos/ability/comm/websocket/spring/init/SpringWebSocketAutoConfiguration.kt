package io.kudos.ability.comm.websocket.spring.init

import io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor
import io.kudos.ability.comm.websocket.common.handler.IKudosWebSocketHandler
import io.kudos.ability.comm.websocket.common.init.WebSocketAutoConfiguration
import io.kudos.ability.comm.websocket.common.session.KudosWebSocketRegistry
import io.kudos.ability.comm.websocket.spring.handler.KudosSpringWebSocketHandler
import io.kudos.ability.comm.websocket.spring.handshake.IWebSocketHandshakeGuard
import io.kudos.ability.comm.websocket.spring.handshake.KudosHandshakeInterceptor
import io.kudos.base.logger.LogFactory
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Registers kudos WebSocket endpoints on a servlet container.
 *
 * The Spring engine module's entry point. The registry and broadcaster themselves come from
 * [WebSocketAutoConfiguration] in `kudos-ability-comm-websocket-common` — they are engine-neutral and
 * shared with the Ktor module — so this class only does what is genuinely Spring's: turn
 * [IKudosWebSocketEndpoint] beans into `WebSocketHandlerRegistry` entries, mount the handshake
 * interceptor, and set the container's WebSocket limits.
 *
 * ## What gets registered
 *
 * Every [IKudosWebSocketEndpoint] bean, in `@Order` sequence. When there are none, a single fallback
 * endpoint is registered at `kudos.ability.comm.websocket.spring.path` **if and only if** the
 * application declares exactly one [IKudosWebSocketHandler] bean — the overwhelmingly common
 * single-endpoint case, without forcing boilerplate for it. Two or more handlers and no endpoint
 * beans is ambiguous, so nothing is registered and a WARN says why; picking one arbitrarily would
 * produce an application that starts fine and silently serves the wrong thing.
 *
 * ## Lifecycle
 *
 * The [KudosSpringWebSocketHandler]s are constructed here rather than declared as beans, because
 * their number and configuration depend on the endpoint beans. That means Spring will not call their
 * `destroy()`, so this configuration is itself a [DisposableBean] and shuts them down — otherwise a
 * context close would leave pump coroutines mid-message and skip every `onDisconnect`, which is
 * exactly the business cleanup (presence keys, audit rows) a rolling restart most needs to run.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@EnableWebSocket
@AutoConfigureAfter(WebSocketAutoConfiguration::class)
@EnableConfigurationProperties(SpringWebSocketProperties::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "kudos.ability.comm.websocket.spring",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@PropertySource(
    value = ["classpath:kudos-ability-comm-websocket-spring.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class SpringWebSocketAutoConfiguration(
    private val properties: SpringWebSocketProperties,
    private val sessionRegistry: KudosWebSocketRegistry,
    private val endpointProvider: ObjectProvider<IKudosWebSocketEndpoint>,
    private val handlerProvider: ObjectProvider<IKudosWebSocketHandler>,
    private val guardProvider: ObjectProvider<IWebSocketHandshakeGuard>,
    private val interceptorProvider: ObjectProvider<IWebSocketConnectInterceptor>,
) : WebSocketConfigurer, IComponentInitializer, DisposableBean {

    private val log = LogFactory.getLog(this::class)

    /** Handlers created in [registerWebSocketHandlers], kept so [destroy] can shut their pumps down. */
    private val started = CopyOnWriteArrayList<KudosSpringWebSocketHandler>()

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        warnIfOriginsAreWideOpen()

        val endpoints = resolveEndpoints()
        if (endpoints.isEmpty()) {
            log.info("No kudos WebSocket endpoint registered: declare an IKudosWebSocketEndpoint bean, " +
                "or a single IKudosWebSocketHandler bean to use the fallback path '{0}'.", properties.path)
            return
        }

        val globalGuards = guardProvider.orderedStream().toList()
        val globalInterceptors = interceptorProvider.orderedStream().toList()

        endpoints.forEach { endpoint ->
            val handler = KudosSpringWebSocketHandler(
                registry = sessionRegistry,
                handler = endpoint.handler,
                sessionFactory = endpoint.sessionFactory,
                connectInterceptors = endpoint.connectInterceptors.ifEmpty { globalInterceptors },
                options = options(),
            )
            started += handler

            val registration = registry
                .addHandler(handler, endpoint.path)
                .addInterceptors(KudosHandshakeInterceptor(endpoint.handshakeGuards.ifEmpty { globalGuards }))
            applyOrigins(registration, endpoint)
            if (endpoint.sockJs ?: properties.sockJs) registration.withSockJS()

            log.info("Registered kudos WebSocket endpoint path={0} handler={1} sockJs={2}",
                endpoint.path, endpoint.handler::class.simpleName, endpoint.sockJs ?: properties.sockJs)
        }
    }

    /**
     * Raises the servlet container's WebSocket buffers above the 8 KB Jakarta default.
     *
     * With `supportsPartialMessages = false` the container is what reassembles fragments, so its
     * buffer size — not anything in this module — decides the largest message an endpoint can accept.
     * Left at the default, a business payload over 8 KB fails at the container with an error that
     * points nowhere near the actual cause.
     *
     * `@ConditionalOnMissingBean` so an application that already tunes the container keeps its own.
     */
    @Bean
    @ConditionalOnMissingBean(ServletServerContainerFactoryBean::class)
    open fun servletServerContainerFactoryBean(): ServletServerContainerFactoryBean =
        ServletServerContainerFactoryBean().apply {
            setMaxTextMessageBufferSize(properties.container.maxTextMessageBufferSize)
            setMaxBinaryMessageBufferSize(properties.container.maxBinaryMessageBufferSize)
            // 0 means "leave the container's own default alone" — passing it through would be read as
            // "time out immediately" / "never time out" depending on the container, neither intended.
            properties.container.maxSessionIdleTimeoutMillis.takeIf { it > 0 }?.let { setMaxSessionIdleTimeout(it) }
            properties.container.asyncSendTimeoutMillis.takeIf { it > 0 }?.let { setAsyncSendTimeout(it) }
        }

    override fun getComponentName() = "kudos-ability-comm-websocket-spring"

    /** Drains and stops every pump created above; see the class KDoc for why this is not Spring's job here. */
    override fun destroy() {
        started.forEach { runCatching { it.destroy() }.onFailure { e -> log.warn("Shutting down a WebSocket handler failed: {0}", e.message) } }
        started.clear()
    }

    /**
     * Endpoint beans if there are any; otherwise the single-handler fallback. See the class KDoc for
     * why "several handlers, no endpoints" registers nothing instead of guessing.
     */
    private fun resolveEndpoints(): List<IKudosWebSocketEndpoint> {
        val declared = endpointProvider.orderedStream().toList()
        if (declared.isNotEmpty()) return declared

        val handlers = handlerProvider.orderedStream().toList()
        return when (handlers.size) {
            0 -> emptyList()
            1 -> listOf(KudosWebSocketEndpoint(properties.path, handlers.single()))
            else -> {
                log.warn("Found {0} IKudosWebSocketHandler beans but no IKudosWebSocketEndpoint bean, so no " +
                    "endpoint was registered — the fallback path '{1}' can only serve one handler. Declare a " +
                    "KudosWebSocketEndpoint bean per handler to say which path each belongs to.",
                    handlers.size, properties.path)
                emptyList()
            }
        }
    }

    /**
     * `internal` rather than private so a test can assert the mapping directly. Bound properties that
     * never reach the object they configure are a bug the container starts happily with, and every
     * limit here — queue capacity, overflow policy, send buffer ceiling — is one whose absence only
     * shows up under production load.
     */
    internal fun options() = KudosSpringWebSocketHandler.Options(
        inboundBufferCapacity = properties.inbound.bufferCapacity,
        inboundOverflow = properties.inbound.overflow,
        sendTimeLimitMillis = properties.send.timeLimitMillis,
        sendBufferSizeLimitBytes = properties.send.bufferSizeLimitBytes,
        sendOverflowStrategy = properties.send.overflowStrategy,
        shutdownTimeoutMillis = properties.shutdownTimeoutMillis,
    )

    /** Endpoint-level origins win; otherwise the module-wide setting; otherwise Spring's same-origin default. */
    private fun applyOrigins(registration: WebSocketHandlerRegistration, endpoint: IKudosWebSocketEndpoint) {
        val origins = endpoint.allowedOrigins ?: properties.allowedOrigins
        if (origins.isNotEmpty()) {
            registration.setAllowedOrigins(*origins.toTypedArray())
            return
        }
        // Patterns are only consulted when no exact list applies, so tightening a deployment means
        // setting one key rather than also having to clear the other.
        if (properties.allowedOriginPatterns.isNotEmpty()) {
            registration.setAllowedOriginPatterns(*properties.allowedOriginPatterns.toTypedArray())
        }
    }

    /**
     * A wildcard origin on a WebSocket endpoint is a genuine vulnerability, not a lax default — see
     * [SpringWebSocketProperties.allowedOrigins]. It stays configurable because local development and
     * gateway-terminated deployments have real uses for it, but it should never be silently in effect
     * in an environment nobody re-checked.
     */
    private fun warnIfOriginsAreWideOpen() {
        val wildcard = "*" in properties.allowedOrigins || "*" in properties.allowedOriginPatterns
        if (wildcard) {
            log.warn("kudos.ability.comm.websocket.spring allows origin '*': any website a logged-in user " +
                "visits can open an authenticated WebSocket to this service. Restrict it outside development.")
        }
    }
}
