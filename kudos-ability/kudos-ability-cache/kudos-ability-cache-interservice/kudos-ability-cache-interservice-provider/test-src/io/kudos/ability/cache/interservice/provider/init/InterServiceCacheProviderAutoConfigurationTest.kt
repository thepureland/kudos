package io.kudos.ability.cache.interservice.provider.init

import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.provider.web.CacheClientRequest
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * test for InterServiceCacheProviderAutoConfiguration
 *
 * Covers (bean factory methods called directly, no Spring container needed):
 * - default properties bean
 * - web filter registration (url pattern, order, filter type) and the wrapAllRequests switch
 * - uid generator bean honoring uidCacheEnabled
 * - aspect bean creation
 * - component name
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class InterServiceCacheProviderAutoConfigurationTest {

    private val configuration = InterServiceCacheProviderAutoConfiguration()

    @Test
    fun interServiceCacheProviderProperties_returnsDefaults() {
        val properties = configuration.interServiceCacheProviderProperties()

        assertFalse(properties.uidCacheEnabled)
        assertFalse(properties.wrapAllRequests)
    }

    @Test
    fun clientCacheWebFilter_registersFilterWithExpectedSettings() {
        val registration = configuration.clientCacheWebFilter(InterServiceCacheProviderProperties())

        assertNotNull(registration.filter)
        assertTrue(registration.urlPatterns.contains("/*"))
        assertEquals(FilterRegistrationBean.HIGHEST_PRECEDENCE, registration.order)
    }

    @Test
    fun clientCacheWebFilter_defaultProperties_doesNotWrapHeaderlessRequests() {
        val registration = configuration.clientCacheWebFilter(InterServiceCacheProviderProperties())
        val chain = RecordingFilterChain()

        registration.filter!!.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertFalse(chain.request is CacheClientRequest)
    }

    @Test
    fun clientCacheWebFilter_wrapAllRequests_wrapsHeaderlessRequests() {
        val properties = InterServiceCacheProviderProperties().apply { wrapAllRequests = true }
        val registration = configuration.clientCacheWebFilter(properties)
        val chain = RecordingFilterChain()

        registration.filter!!.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertTrue(chain.request is CacheClientRequest)
    }

    @Test
    fun clientCacheUidGenerator_honorsUidCacheEnabled() {
        val cachingGenerator = configuration.clientCacheUidGenerator(
            InterServiceCacheProviderProperties().apply { uidCacheEnabled = true }
        )
        val plainGenerator = configuration.clientCacheUidGenerator(InterServiceCacheProviderProperties())

        val obj = StableKeyPayload("one")
        val initialUid = cachingGenerator.generate(obj)
        obj.payload = "mutated"

        // Caching generator keeps the equality-keyed uid; plain generator recomputes.
        assertEquals(initialUid, cachingGenerator.generate(obj))
        assertEquals(ClientCacheItem.genUid(obj), plainGenerator.generate(obj))
    }

    @Test
    fun clientCacheableAspect_isCreatedFromGenerator() {
        val generator = configuration.clientCacheUidGenerator(InterServiceCacheProviderProperties())

        assertNotNull(configuration.clientCacheableAspect(generator))
    }

    @Test
    fun getComponentName_matchesModuleName() {
        assertEquals("kudos-ability-cache-interservice-provider", configuration.getComponentName())
    }

    /**
     * Mutable payload whose equals/hashCode never change, so the equality-keyed uid cache keeps
     * hitting the same entry even after the JSON content changes.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class StableKeyPayload(var payload: String) {
        override fun equals(other: Any?): Boolean = other is StableKeyPayload
        override fun hashCode(): Int = 0
    }

    /**
     * Filter chain stub that records the inbound request object.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingFilterChain : FilterChain {
        var request: ServletRequest? = null
            private set

        override fun doFilter(request: ServletRequest?, response: ServletResponse?) {
            this.request = request
        }
    }
}
