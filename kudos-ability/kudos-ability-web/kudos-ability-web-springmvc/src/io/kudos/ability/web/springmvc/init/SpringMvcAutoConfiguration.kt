package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.springmvc.filter.IWebContextInitFilter
import io.kudos.ability.web.springmvc.filter.WebContextInitFilter
import io.kudos.ability.web.springmvc.handler.BadRequestExceptionHandler
import io.kudos.ability.web.springmvc.handler.GlobalExceptionHandler
import io.kudos.ability.web.springmvc.handler.GlobalResponseBodyHandler
import io.kudos.ability.web.springmvc.handler.MutableListSearchPayloadGuardAdvice
import io.kudos.base.logger.LogFactory
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import jakarta.servlet.Filter
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.SearchStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jetty.JettyServerCustomizer
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory
import org.springframework.boot.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.tomcat.TomcatContextCustomizer
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.validation.Validator
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.util.EventListener


/**
 * Spring MVC auto-configuration class.
 *
 * **On `@ConditionalOnMissingBean` in this class.** Every conditional here names either a concrete type or an
 * explicit bean name, never a broad framework interface. The distinction is not cosmetic: the annotation matches
 * on the factory method's *return type*, so a method returning `FilterRegistrationBean<*>` backs off as soon as
 * the application registers any filter of its own, and one returning `HandlerInterceptor` backs off on any
 * interceptor at all. Both would disable this module's wiring silently, with nothing in the logs to explain why
 * the request context stopped being populated.
 *
 * Note also that this class is imported by `ComponentInitializerSelector`, a plain (non-deferred) `ImportSelector`,
 * so its bean definitions are registered *before* auto-configurations are evaluated. That ordering is what lets a
 * conditional here win over Spring Boot's equivalent; it is also why a conditional here cannot reliably see beans
 * the application declares in its own `@Configuration` classes. Where "the application may override this" has to
 * hold, the override point is an interface bean ([IWebContextInitFilter], [ObjectMapper]) rather than a registration
 * wrapper.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@PropertySource(
    value = ["classpath:kudos-ability-web-springmvc.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableConfigurationProperties(SpringMvcProperties::class)
@EnableWebMvc
open class SpringMvcAutoConfiguration(
    private val properties: SpringMvcProperties,
    /**
     * Resolved lazily rather than by calling the [objectMapper] factory method directly.
     *
     * A `@Configuration` proxy only returns the container singleton for a `@Bean` method whose bean definition
     * actually exists. When [objectMapper]'s `@ConditionalOnMissingBean` backs off — precisely the case where the
     * application supplied its own mapper — the proxy falls through to the method body and constructs a fresh,
     * unmanaged instance. Wiring the converters to *that* would silently ignore the application's mapper while
     * appearing to work.
     */
    private val objectMapperProvider: ObjectProvider<ObjectMapper>
) : WebMvcConfigurer, IComponentInitializer {

    private val log = LogFactory.getLog(this::class)

    /**
     * Replace Spring MVC's default mvcValidator with the project-level Validator (from kudos-context's
     * [io.kudos.context.init.ValidatorAutoConfiguration]). `required = false` ensures that if the upstream removes
     * ValidatorAutoConfiguration, the app can still start and fall back to Spring's default validator.
     */
    @Autowired(required = false)
    private var kudosValidator: LocalValidatorFactoryBean? = null

    /**
     * Override [WebMvcConfigurer.getValidator] to replace the MVC default validator with [kudosValidator].
     * Returning null is equivalent to "use Spring's default", which pairs well with `required = false` on [kudosValidator].
     *
     * @return the project-level validator; null when unconfigured (Spring MVC falls back on its own)
     * @author K
     * @since 1.0.0
     */
    override fun getValidator(): Validator? = kudosValidator

    /** Provides the listener required by `RequestContextHolder` when ServletRequestListener fires. */
    @Bean
    @ConditionalOnMissingBean(RequestContextListener::class)
    open fun requestContextListener(): RequestContextListener = RequestContextListener()

    /** Explicitly register [RequestContextListener] with the servlet container (order 1, right after container initialization). */
    @Bean
    @ConditionalOnMissingBean(name = ["requestContextListenerRegistration"])
    open fun requestContextListenerRegistration(
        requestContextListener: RequestContextListener
    ): ServletListenerRegistrationBean<EventListener> {
        val registrationBean = ServletListenerRegistrationBean<EventListener>()
        registrationBean.setListener(requestContextListener)
        registrationBean.order = 1
        return registrationBean
    }

    /** Kudos context initialization filter (business code can override via a custom [IWebContextInitFilter] bean). */
    @Bean
    @ConditionalOnMissingBean(IWebContextInitFilter::class)
    open fun webContextInitFilter(): IWebContextInitFilter = WebContextInitFilter(properties.context)

    /**
     * Register [IWebContextInitFilter]: order is [SessionRepositoryFilter.DEFAULT_ORDER] + 1,
     * so the kudos context filter only captures sessionAttributes after the session is loaded.
     */
    @Bean
    @ConditionalOnMissingBean(name = ["contextInitFilterRegistration"])
    open fun contextInitFilterRegistration(contextInitFilter: IWebContextInitFilter): FilterRegistrationBean<Filter> {
        val registration = FilterRegistrationBean<Filter>()
        registration.setFilter(contextInitFilter)
        registration.addUrlPatterns("/*")
        registration.setName("contextInitFilter")
        registration.order = SessionRepositoryFilter.DEFAULT_ORDER + 1
        return registration
    }

    /**
     * Default Jackson 3 [ObjectMapper]; business code may override via a custom bean to inject
     * custom modules / time-zone strategies.
     *
     * **Modules are discovered, not omitted.** A bare `ObjectMapper()` registers nothing, and the
     * one that matters here is the Kotlin module: a Kotlin data class has no no-arg constructor, so
     * without it Jackson cannot construct one and every `@RequestBody` taking a data class fails
     * with "no Creators, like default constructor, exist". That failure is invisible to unit tests,
     * which call services directly and never cross the HTTP boundary — it only appears the first
     * time somebody POSTs real JSON.
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper::class)
    open fun objectMapper(): JsonMapper = JsonMapper.builder().findAndAddModules().build()

    /**
     * Makes the MVC message converters use the container's [ObjectMapper].
     *
     * Necessary because [EnableWebMvc] takes over converter construction from Spring Boot: the framework builds
     * its own Jackson converter with its own mapper, so configuring the bean alone would leave request bodies
     * parsed by a mapper nobody configured.
     *
     * `withJsonConverter` substitutes the JSON converter **in its existing slot**. That placement is
     * load-bearing rather than incidental: a JSON converter positioned ahead of `StringHttpMessageConverter`
     * also claims `String` return values, which then come back JSON-quoted — a subtle break in every endpoint
     * returning a bare string. Expressing the substitution through the builder lets the framework preserve the
     * ordering instead of this module hand-maintaining an index into the converter list.
     */
    override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
        val mapper = objectMapperProvider.getIfAvailable()
        if (mapper == null) {
            // No unique ObjectMapper to adopt. Leaving Spring's own converter in place is the safe outcome:
            // the alternative is failing MVC configuration over an ambiguity the application can resolve
            // with @Primary.
            return
        }
        if (mapper !is JsonMapper) {
            // JacksonJsonHttpMessageConverter is built from a JsonMapper specifically. Substituting is not
            // possible, and doing it silently wrong would be worse than not doing it.
            log.warn(
                "The configured ObjectMapper (${mapper::class.java.name}) is not a JsonMapper, so the MVC " +
                    "JSON converter cannot adopt it. Request/response bodies will use Spring's own mapper."
            )
            return
        }
        builder.withJsonConverter(JacksonJsonHttpMessageConverter(mapper))
    }

    /** Unified response handler for bad request errors (parameter / binding / Bean Validation errors). */
    @Bean
    @ConditionalOnMissingBean(BadRequestExceptionHandler::class)
    open fun badRequestExceptionHandler(): BadRequestExceptionHandler = BadRequestExceptionHandler()

    /** Unified response handler for business exceptions / uncaught exceptions (complements [badRequestExceptionHandler] and covers the remaining cases). */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler::class)
    open fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()

    /** ResponseBodyAdvice that wraps controller return values uniformly into `ApiResponse`. */
    @Bean
    @ConditionalOnMissingBean(GlobalResponseBodyHandler::class)
    open fun globalResponseBodyHandler(objectMapper: ObjectMapper): GlobalResponseBodyHandler =
        GlobalResponseBodyHandler(objectMapper)

    /** Security guard that rejects [io.kudos.base.model.payload.MutableListSearchPayload] as a request body. */
    @Bean
    @ConditionalOnMissingBean(MutableListSearchPayloadGuardAdvice::class)
    open fun mutableListSearchPayloadGuardAdvice(): MutableListSearchPayloadGuardAdvice =
        MutableListSearchPayloadGuardAdvice()

    /**
     * Registers the framework-level CORS support, driven by [SpringMvcProperties.Cors].
     *
     * The single previous implementation was doubled up with a hand-rolled interceptor that reflected the
     * request's `Origin` header and advertised `Access-Control-Allow-Methods: *`, overwriting whatever this
     * mapping had negotiated. That interceptor has been removed: Spring's own support already handles preflight,
     * `Vary` headers and credential rules correctly, and two writers disagreeing about the same response headers
     * is a bug waiting to be debugged.
     *
     * Defaults remain permissive so CORS never blocks development, and [warnIfCorsWideOpen] makes the risk
     * visible when they survive into a deployed environment.
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        val cors = properties.cors
        if (!cors.enabled) {
            log.info("CORS mapping disabled via kudos.ability.web.springmvc.cors.enabled=false")
            return
        }
        val registration = registry.addMapping(cors.pathPattern)
            .allowedMethods(*cors.allowedMethods.toTypedArray())
            .allowedHeaders(*cors.allowedHeaders.toTypedArray())
            .allowCredentials(cors.allowCredentials)
            .maxAge(cors.maxAge)
        if (cors.allowedOrigins.isNotEmpty()) {
            registration.allowedOrigins(*cors.allowedOrigins.toTypedArray())
        } else {
            registration.allowedOriginPatterns(*cors.allowedOriginPatterns.toTypedArray())
        }
        if (cors.exposedHeaders.isNotEmpty()) {
            registration.exposedHeaders(*cors.exposedHeaders.toTypedArray())
        }
        warnIfCorsWideOpen(cors)
    }

    /**
     * Log a warning when the effective CORS policy reflects any origin *and* permits credentials.
     *
     * That pair is accepted by Spring but is equivalent to "any website may call this API as the logged-in user",
     * so it must not pass unnoticed into an environment that believed it was protected.
     *
     * @param cors the effective CORS settings
     * @author K
     * @since 1.0.0
     */
    private fun warnIfCorsWideOpen(cors: SpringMvcProperties.Cors) {
        val reflectsAnyOrigin = cors.allowedOrigins.isEmpty() && cors.allowedOriginPatterns.contains("*")
        if (reflectsAnyOrigin && cors.allowCredentials) {
            log.warn(
                "CORS is configured to reflect ANY origin while allowing credentials " +
                    "(kudos.ability.web.springmvc.cors.allowed-origin-patterns=[*], allow-credentials=true). " +
                    "This is a development-friendly default and is unsafe for production: set " +
                    "kudos.ability.web.springmvc.cors.allowed-origins to an explicit whitelist."
            )
        }
    }

    override fun getComponentName() = "kudos-ability-web-springmvc"

    /**
     * Tomcat-specific wiring.
     *
     * Split into a nested `@ConditionalOnClass` configuration so that an application which swaps Tomcat out for
     * another container never loads these types. The previous implementation imported Tomcat classes
     * unconditionally, which meant a Jetty-only deployment failed at class-initialization time.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(TomcatServletWebServerFactory::class)
    open class TomcatConfiguration {

        /**
         * Relax the connector's path/query character rules.
         *
         * Contributed as a [TomcatConnectorCustomizer] **bean** rather than applied to a factory this module
         * constructs, so it reaches the connector regardless of who built the factory — Spring Boot's own
         * auto-configuration collects `TomcatConnectorCustomizer` beans exactly the same way [forcedTomcatFactory]
         * does. Without that indirection the relaxation would silently stop applying whenever the container came
         * from Boot instead of from here.
         *
         * @param properties module properties supplying the character sets
         * @return the connector customizer
         * @author K
         * @since 1.0.0
         */
        @Bean
        @ConditionalOnMissingBean(name = ["kudosTomcatRelaxedCharsCustomizer"])
        open fun kudosTomcatRelaxedCharsCustomizer(properties: SpringMvcProperties): TomcatConnectorCustomizer =
            TomcatConnectorCustomizer { connector ->
                connector.setProperty("relaxedPathChars", properties.tomcat.relaxedPathChars)
                connector.setProperty("relaxedQueryChars", properties.tomcat.relaxedQueryChars)
            }

        /**
         * Force Tomcat when `kudos.ability.web.springmvc.server=TOMCAT` is set explicitly.
         *
         * Returns Spring Boot's own [TomcatServletWebServerFactory] — a `ConfigurableServletWebServerFactory` —
         * rather than a hand-written delegating wrapper. That distinction restores every `server.*` property:
         * Boot applies them through `WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>` beans, which
         * a wrapper type simply does not match, so port aside, `server.ssl.*`, `server.compression.*`,
         * `server.servlet.session.*`, `server.shutdown` and the rest were being dropped without a word.
         *
         * The customizer collections are forwarded for the same reason Boot forwards them: a factory built here
         * would otherwise ignore any `TomcatConnectorCustomizer` / `TomcatContextCustomizer` /
         * `TomcatProtocolHandlerCustomizer` bean the application contributes.
         *
         * @return the Tomcat factory, pre-loaded with the context's Tomcat customizer beans
         * @author K
         * @since 1.0.0
         */
        @Bean
        @ConditionalOnProperty(name = ["kudos.ability.web.springmvc.server"], havingValue = "TOMCAT")
        @ConditionalOnMissingBean(value = [ServletWebServerFactory::class], search = SearchStrategy.CURRENT)
        open fun forcedTomcatFactory(
            connectorCustomizers: ObjectProvider<TomcatConnectorCustomizer>,
            contextCustomizers: ObjectProvider<TomcatContextCustomizer>,
            protocolHandlerCustomizers: ObjectProvider<TomcatProtocolHandlerCustomizer<*>>
        ): TomcatServletWebServerFactory {
            val factory = TomcatServletWebServerFactory()
            factory.connectorCustomizers.addAll(connectorCustomizers.orderedStream().toList())
            factory.contextCustomizers.addAll(contextCustomizers.orderedStream().toList())
            factory.protocolHandlerCustomizers.addAll(protocolHandlerCustomizers.orderedStream().toList())
            return factory
        }

    }

    /**
     * Jetty-specific wiring.
     *
     * Jetty is a `compileOnly` dependency of this module: the types below are needed to express the
     * configuration, but must not be forced onto applications that run Tomcat. `@ConditionalOnClass` keeps the
     * class unloaded when Jetty is absent, which is what makes that dependency scope safe — and replaces the
     * `Class.forName` reflection the previous implementation needed for the same reason.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JettyServletWebServerFactory::class)
    open class JettyConfiguration {

        /**
         * Force Jetty when `kudos.ability.web.springmvc.server=JETTY` is set explicitly.
         *
         * @return the Jetty factory, pre-loaded with the context's Jetty server customizer beans
         * @author K
         * @since 1.0.0
         */
        @Bean
        @ConditionalOnProperty(name = ["kudos.ability.web.springmvc.server"], havingValue = "JETTY")
        @ConditionalOnMissingBean(value = [ServletWebServerFactory::class], search = SearchStrategy.CURRENT)
        open fun forcedJettyFactory(
            serverCustomizers: ObjectProvider<JettyServerCustomizer>
        ): JettyServletWebServerFactory {
            val factory = JettyServletWebServerFactory()
            factory.serverCustomizers.addAll(serverCustomizers.orderedStream().toList())
            return factory
        }

    }

}
