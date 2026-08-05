package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.springmvc.filter.WebContextInitFilter
import io.kudos.ability.web.springmvc.interceptor.CorsHandlerInterceptor
import org.springframework.mock.env.MockEnvironment
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [SpringMvcAutoConfiguration].
 *
 * Covers every bean factory method, validator delegation (null and non-null),
 * filter/listener registration metadata (order, url patterns, name),
 * interceptor registration, the default CORS mapping, and the component name.
 *
 * @author K
 * @since 1.0.0
 */
internal class SpringMvcAutoConfigurationTest {

    private val config = SpringMvcAutoConfiguration()

    @Test
    fun getValidator_returnsNullWhenKudosValidatorAbsent() {
        assertNull(config.validator)
    }

    @Test
    fun getValidator_returnsKudosValidatorWhenInjected() {
        val validator = LocalValidatorFactoryBean()
        val field = SpringMvcAutoConfiguration::class.java.getDeclaredField("kudosValidator")
        field.isAccessible = true
        field.set(config, validator)

        assertSame(validator, config.validator)
    }

    @Test
    fun webServerFactory_returnsSwitchingFactory() {
        val factory = config.webServerFactory(MockEnvironment())
        assertTrue(factory is SwitchingServletWebServerFactory)
    }

    @Test
    fun servletListenerRegistration_registersRequestContextListenerWithOrder1() {
        val listener = config.requestContextListener()
        val registration = config.servletListenerRegistration(listener)

        assertSame(listener, registration.listener)
        assertEquals(1, registration.order)
    }

    @Test
    fun webContextInitFilter_defaultsToWebContextInitFilter() {
        assertTrue(config.webContextInitFilter() is WebContextInitFilter)
    }

    @Test
    fun registerAuthFilter_registersContextInitFilterAfterSessionRepositoryFilter() {
        val filter = config.webContextInitFilter()
        val registration = config.registerAuthFilter(filter)

        assertSame(filter, registration.filter)
        assertEquals(SessionRepositoryFilter.DEFAULT_ORDER + 1, registration.order)
        assertTrue(registration.urlPatterns.contains("/*"))
    }

    @Test
    fun simpleBeanFactories_returnExpectedTypes() {
        assertTrue(config.corsHandlerInterceptor() is CorsHandlerInterceptor)
        assertNotNull(config.objectMapper())
        assertNotNull(config.badRequestExceptionHandler())
        assertNotNull(config.globalExceptionHandler())
        assertNotNull(config.globalResponseBodyHandler(config.objectMapper()))
        assertNotNull(config.mutableListSearchPayloadGuardAdvice())
    }

    @Test
    fun addInterceptors_registersCorsHandlerInterceptor() {
        val registry = ExposedInterceptorRegistry()
        config.addInterceptors(registry)

        assertTrue(registry.registered().any {
            it is CorsHandlerInterceptor ||
                (it is org.springframework.web.servlet.handler.MappedInterceptor && it.interceptor is CorsHandlerInterceptor)
        })
    }

    @Test
    fun addCorsMappings_opensAllPatternsWithCredentials() {
        val registry = ExposedCorsRegistry()
        config.addCorsMappings(registry)

        val cors = assertNotNull(registry.configurations()["/**"])
        assertEquals(listOf("*"), cors.allowedOriginPatterns)
        assertEquals(true, cors.allowCredentials)
        assertEquals(listOf("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD"), cors.allowedMethods)
        assertEquals(3600L * 24, cors.maxAge)
    }

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ability-web-springmvc", config.getComponentName())
    }

    /** Exposes the protected interceptor list for assertions. */
    private class ExposedInterceptorRegistry : InterceptorRegistry() {
        fun registered(): List<Any> = interceptors
    }

    /** Exposes the protected CORS configuration map for assertions. */
    private class ExposedCorsRegistry : CorsRegistry() {
        fun configurations() = corsConfigurations
    }

}
