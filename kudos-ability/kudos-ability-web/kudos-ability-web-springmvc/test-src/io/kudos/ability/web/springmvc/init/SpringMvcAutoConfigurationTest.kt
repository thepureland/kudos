package io.kudos.ability.web.springmvc.init

import io.kudos.ability.web.springmvc.filter.WebContextInitFilter
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.mock.http.MockHttpOutputMessage
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [SpringMvcAutoConfiguration].
 *
 * Covers every bean factory method, validator delegation (null and non-null), filter/listener registration
 * metadata (order, url patterns, name), message-converter substitution, and CORS mapping driven by
 * [SpringMvcProperties].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SpringMvcAutoConfigurationTest {

    private val properties = SpringMvcProperties()

    private fun config(
        props: SpringMvcProperties = properties,
        mapper: ObjectMapper? = JsonMapper.builder().build()
    ) = SpringMvcAutoConfiguration(props, SingletonObjectProvider(mapper))

    @Test
    fun getValidator_returnsNullWhenKudosValidatorAbsent() {
        assertNull(config().validator)
    }

    @Test
    fun getValidator_returnsKudosValidatorWhenInjected() {
        val configuration = config()
        val validator = LocalValidatorFactoryBean()
        val field = SpringMvcAutoConfiguration::class.java.getDeclaredField("kudosValidator")
        field.isAccessible = true
        field.set(configuration, validator)

        assertSame(validator, configuration.validator)
    }

    @Test
    fun requestContextListenerRegistration_registersListenerWithOrder1() {
        val configuration = config()
        val listener = configuration.requestContextListener()
        val registration = configuration.requestContextListenerRegistration(listener)

        assertSame(listener, registration.listener)
        assertEquals(1, registration.order)
    }

    @Test
    fun webContextInitFilter_defaultsToWebContextInitFilter() {
        assertTrue(config().webContextInitFilter() is WebContextInitFilter)
    }

    @Test
    fun contextInitFilterRegistration_registersFilterAfterSessionRepositoryFilter() {
        val configuration = config()
        val filter = configuration.webContextInitFilter()
        val registration = configuration.contextInitFilterRegistration(filter)

        assertSame(filter, registration.filter)
        assertEquals(SessionRepositoryFilter.DEFAULT_ORDER + 1, registration.order)
        assertTrue(registration.urlPatterns.contains("/*"))
    }

    @Test
    fun simpleBeanFactories_returnExpectedTypes() {
        val configuration = config()
        assertNotNull(configuration.objectMapper())
        assertNotNull(configuration.badRequestExceptionHandler())
        assertNotNull(configuration.globalExceptionHandler())
        assertNotNull(configuration.globalResponseBodyHandler(configuration.objectMapper()))
        assertNotNull(configuration.mutableListSearchPayloadGuardAdvice())
    }

    /**
     * The container's mapper must actually be the one the MVC JSON converter writes with.
     *
     * Asserted behaviourally — the mapper is given a setting whose effect is visible in the output — because
     * the converter exposes no accessor for its mapper, and an assertion that merely counted converters would
     * pass just as happily against Spring's own default mapper.
     */
    @Test
    fun configureMessageConverters_substitutesJsonConverterWithContainerMapper() {
        val mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()

        val json = buildJsonConverter(config(mapper = mapper))

        val output = MockHttpOutputMessage()
        @Suppress("UNCHECKED_CAST")
        (json as HttpMessageConverter<Any>).write(mapOf("a" to 1), MediaType.APPLICATION_JSON, output)
        assertTrue(
            output.bodyAsString.contains("\n"),
            "Expected the container's INDENT_OUTPUT mapper to be in use, got: ${output.bodyAsString}"
        )
    }

    /**
     * The JSON converter must stay **behind** [StringHttpMessageConverter]. Promoted ahead of it, a JSON
     * converter also claims `String` return values and hands them back JSON-quoted — a subtle break in every
     * endpoint that returns a bare string.
     */
    @Test
    fun configureMessageConverters_keepsJsonConverterBehindStringConverter() {
        val converters = buildConverters(config()).toList()

        val stringAt = converters.indexOfFirst { it is StringHttpMessageConverter }
        val jsonAt = converters.indexOfFirst { it is JacksonJsonHttpMessageConverter }
        assertTrue(stringAt >= 0 && jsonAt >= 0, "Both converters should be registered")
        assertTrue(stringAt < jsonAt, "The JSON converter must not be promoted ahead of the String converter")
    }

    /**
     * With no unique `ObjectMapper` in the container there is nothing to rewire; leaving Spring's own converter
     * untouched is the safe outcome, and is what keeps the ambiguity from becoming a startup failure here.
     */
    @Test
    fun configureMessageConverters_leavesConvertersAloneWhenNoMapperAvailable() {
        val json = buildJsonConverter(config(mapper = null))

        val output = MockHttpOutputMessage()
        @Suppress("UNCHECKED_CAST")
        (json as HttpMessageConverter<Any>).write(mapOf("a" to 1), MediaType.APPLICATION_JSON, output)
        assertTrue(!output.bodyAsString.contains("\n"), "Spring's own default mapper should still be in use")
    }

    /** Run the configurer against a real server builder and return the resulting converters. */
    private fun buildConverters(configuration: SpringMvcAutoConfiguration): HttpMessageConverters {
        val builder = HttpMessageConverters.forServer().registerDefaults()
        configuration.configureMessageConverters(builder)
        return builder.build()
    }

    /** Run the configurer against a real server builder and return the single registered JSON converter. */
    private fun buildJsonConverter(configuration: SpringMvcAutoConfiguration): JacksonJsonHttpMessageConverter =
        buildConverters(configuration).filterIsInstance<JacksonJsonHttpMessageConverter>().single()

    @Test
    fun addCorsMappings_appliesPermissiveDefaults() {
        val registry = ExposedCorsRegistry()
        config().addCorsMappings(registry)

        val cors = assertNotNull(registry.configurations()["/**"])
        assertEquals(listOf("*"), cors.allowedOriginPatterns)
        assertEquals(true, cors.allowCredentials)
        assertEquals(listOf("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS", "HEAD"), cors.allowedMethods)
        assertEquals(3600L * 24, cors.maxAge)
    }

    /** An explicit origin whitelist must take precedence over — and fully suppress — the wildcard pattern default. */
    @Test
    fun addCorsMappings_explicitOriginsSuppressWildcardPatterns() {
        val props = SpringMvcProperties().apply {
            cors.allowedOrigins = listOf("https://kudos.example")
            cors.exposedHeaders = listOf("X-Trace-Id")
        }
        val registry = ExposedCorsRegistry()
        config(props).addCorsMappings(registry)

        val cors = assertNotNull(registry.configurations()["/**"])
        assertEquals(listOf("https://kudos.example"), cors.allowedOrigins)
        assertNull(cors.allowedOriginPatterns)
        assertEquals(listOf("X-Trace-Id"), cors.exposedHeaders)
    }

    @Test
    fun addCorsMappings_registersNothingWhenDisabled() {
        val props = SpringMvcProperties().apply { cors.enabled = false }
        val registry = ExposedCorsRegistry()
        config(props).addCorsMappings(registry)

        assertTrue(registry.configurations().isEmpty())
    }

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ability-web-springmvc", config().getComponentName())
    }

    /** Exposes the protected CORS configuration map for assertions. */
    private class ExposedCorsRegistry : CorsRegistry() {
        fun configurations() = corsConfigurations
    }

    /** Minimal [ObjectProvider] returning a fixed instance (or nothing, to model an unresolvable dependency). */
    private class SingletonObjectProvider<T : Any>(private val instance: T?) : ObjectProvider<T> {
        override fun getObject(): T = instance ?: error("No instance configured")
        override fun getObject(vararg args: Any?): T = getObject()
        override fun getIfAvailable(): T? = instance
        override fun getIfUnique(): T? = instance
    }

}
