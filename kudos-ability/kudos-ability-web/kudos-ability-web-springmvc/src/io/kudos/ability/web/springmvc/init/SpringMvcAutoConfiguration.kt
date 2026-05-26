package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.springmvc.filter.IWebContextInitFilter
import io.kudos.ability.web.springmvc.filter.WebContextInitFilter
import io.kudos.ability.web.springmvc.handler.BadRequestExceptionHandler
import io.kudos.ability.web.springmvc.handler.GlobalExceptionHandler
import io.kudos.ability.web.springmvc.handler.GlobalResponseBodyHandler
import io.kudos.ability.web.springmvc.handler.MutableListSearchPayloadGuardAdvice
import io.kudos.ability.web.springmvc.interceptor.CorsHandlerInterceptor
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import jakarta.servlet.Filter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.server.servlet.ServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.validation.Validator
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.context.request.RequestContextListener
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper
import java.util.EventListener


/**
 * Spring MVC auto-configuration class.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@PropertySource(
    value = ["classpath:kudos-ability-web-springmvc.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableWebMvc
open class SpringMvcAutoConfiguration : WebMvcConfigurer, IComponentInitializer {

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

    /** Embedded container factory: pick Tomcat / Jetty based on `kudos.ability.web.springmvc.server`. */
    @Bean
    @ConditionalOnMissingBean
    open fun webServerFactory(env: Environment): ServletWebServerFactory = SwitchingServletWebServerFactory(env)

    /** Provides the listener required by `RequestContextHolder` when ServletRequestListener fires. */
    @Bean
    open fun requestContextListener(): RequestContextListener = RequestContextListener()

    /** Explicitly register [RequestContextListener] with the servlet container (order 1, right after container initialization). */
    @Bean
    open fun servletListenerRegistration(requestContextListener: RequestContextListener): ServletListenerRegistrationBean<EventListener> {
        val registrationBean = ServletListenerRegistrationBean<EventListener>()
        registrationBean.setListener(requestContextListener)
        registrationBean.order = 1
        return registrationBean
    }

    /** Kudos context initialization filter (business code can override via a custom [IWebContextInitFilter] bean). */
    @Bean
    @ConditionalOnMissingBean
    open fun webContextInitFilter(): IWebContextInitFilter = WebContextInitFilter()

    /**
     * Register [IWebContextInitFilter]: order is [SessionRepositoryFilter.DEFAULT_ORDER] + 1,
     * so the kudos context filter only captures sessionAttributes after the session is loaded.
     */
    @Bean
    @ConditionalOnMissingBean
    open fun registerAuthFilter(contextInitFilter: IWebContextInitFilter): FilterRegistrationBean<*> {
        val registration = FilterRegistrationBean<Filter>()
        registration.setFilter(contextInitFilter)
        registration.addUrlPatterns("/*")
        registration.setName("contextInitFilter")
        registration.order = SessionRepositoryFilter.DEFAULT_ORDER + 1
        return registration
    }

    /** CORS header injection interceptor (complements the Spring CORS support provided by [addCorsMappings] below). */
    @Bean
    @ConditionalOnMissingBean
    open fun corsHandlerInterceptor(): HandlerInterceptor = CorsHandlerInterceptor()

    /** Default Jackson 3 [ObjectMapper]; business code may override via a custom bean to inject custom modules / time-zone strategies. */
    @Bean
    @ConditionalOnMissingBean
    open fun objectMapper(): ObjectMapper = ObjectMapper()

    /** Unified response handler for bad request errors (parameter / binding / Bean Validation errors). */
    @Bean
    @ConditionalOnMissingBean
    open fun badRequestExceptionHandler(): BadRequestExceptionHandler = BadRequestExceptionHandler()

    /** Unified response handler for business exceptions / uncaught exceptions (complements [badRequestExceptionHandler] and covers the remaining cases). */
    @Bean
    @ConditionalOnMissingBean
    open fun globalExceptionHandler(): GlobalExceptionHandler = GlobalExceptionHandler()

    /** ResponseBodyAdvice that wraps controller return values uniformly into `ApiResponse`. */
    @Bean
    @ConditionalOnMissingBean
    open fun globalResponseBodyHandler(objectMapper: ObjectMapper): GlobalResponseBodyHandler =
        GlobalResponseBodyHandler(objectMapper)

    /** Security guard that rejects [io.kudos.base.model.payload.MutableListSearchPayload] as a request body. */
    @Bean
    @ConditionalOnMissingBean
    open fun mutableListSearchPayloadGuardAdvice(): MutableListSearchPayloadGuardAdvice =
        MutableListSearchPayloadGuardAdvice()

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(corsHandlerInterceptor()).addPathPatterns("/**")
    }

    /**
     * Open CORS by default: all origin patterns + credentials.
     *
     * **Production deployments should override this via a custom [WebMvcConfigurer]**: `allowedOriginPatterns("*") +
     * allowCredentials(true)` is legal in Spring, but it is equivalent to "reflect any origin + allow Cookies",
     * which is effectively an open gate. The framework loosens the defaults so that CORS does not block development;
     * tighten to specific domains in production.
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowCredentials(true)
            .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD")
            .maxAge(3600 * 24)
    }

    override fun getComponentName() = "kudos-ability-web-springmvc"

}
