package io.kudos.ability.web.guest.init

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.web.guest.filter.GuestAccessFilter
import io.kudos.ability.web.guest.init.properties.GuestProperties
import io.kudos.ability.web.guest.provider.GuestAccess
import io.kudos.ability.web.guest.provider.GuestAccessService
import io.kudos.ability.web.guest.provider.GuestAccessUniqueKey
import io.kudos.ability.web.guest.provider.IGuestAccessListener
import io.kudos.ability.web.guest.provider.IGuestAccessService
import io.kudos.ability.web.guest.provider.IGuestAccessStore
import io.kudos.ability.web.guest.provider.RedisGuestAccessStore
import org.mockito.Mockito
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [GuestAutoConfiguration]'s bean factory methods, invoked directly without a
 * Spring context.
 *
 * Verifies that:
 *  - every `@Bean` method returns the documented default implementation type
 *    (unique-key → [GuestAccessUniqueKey], service → [GuestAccessService],
 *    store → [RedisGuestAccessStore], filter → [GuestAccessFilter]);
 *  - the store bean resolves the optional [IGuestAccessListener] from the [ObjectProvider]
 *    both when a listener bean exists and when none is registered (ifAvailable == null);
 *  - the produced beans are actually functional (the unique-key bean hashes a request, the
 *    service bean reflects the enabled flag);
 *  - [GuestAutoConfiguration.getComponentName] reports the module name used by the kudos
 *    component-initializer banner.
 *
 * The `@ConditionalOnMissingBean` / `@PropertySource` wiring itself is Spring-framework
 * behaviour and is not re-tested here.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class GuestAutoConfigurationTest {

    private val config = GuestAutoConfiguration()
    private val properties = GuestProperties()

    @Suppress("UNCHECKED_CAST")
    private fun redisTemplates(): RedisTemplates {
        val template = Mockito.mock(RedisTemplate::class.java) as RedisTemplate<Any, Any?>
        return RedisTemplates(mutableMapOf(), template)
    }

    @Test
    fun guestAccessUniqueKey_returnsDefaultMd5Implementation() {
        val bean = config.guestAccessUniqueKey(properties)
        assertIs<GuestAccessUniqueKey>(bean)
        // The bean must be functional, not just constructible.
        val hash = bean.gen(MockHttpServletRequest().apply { remoteAddr = "10.0.0.9" })
        assertNotNull(hash)
        assertEquals(32, hash.length, "default fingerprint is an MD5 hex digest (32 chars)")
    }

    @Test
    fun guestAccessService_returnsDefaultServiceWiredToProperties() {
        properties.enabled = true
        val bean = config.guestAccessService(properties, { "h" }, emptyProvider())
        assertIs<GuestAccessService>(bean)
        assertEquals(true, bean.isEnabled(), "service bean must reflect the bound properties")
        properties.enabled = false
        assertEquals(false, bean.isEnabled(), "enabled flag must be read live, not cached at wiring time")
    }

    @Test
    fun guestAccessStore_returnsRedisStore_withListenerWhenRegistered() {
        val listener = IGuestAccessListener { /* no-op */ }
        val bean = config.guestAccessStore(properties, redisTemplates(), providerOf(listener))
        assertIs<RedisGuestAccessStore>(bean)
    }

    @Test
    fun guestAccessStore_returnsRedisStore_whenNoListenerBeanExists() {
        // ObjectProvider.ifAvailable == null must wire cleanly — listener is an optional SPI.
        val bean = config.guestAccessStore(properties, redisTemplates(), emptyProvider())
        assertIs<RedisGuestAccessStore>(bean)
    }

    @Test
    fun guestAccessFilter_returnsFilterDrivingTheGivenService() {
        val service = object : IGuestAccessService {
            var enabledCalls = 0
            override fun isEnabled(): Boolean {
                enabledCalls++
                return false
            }

            override fun isExclude(request: jakarta.servlet.http.HttpServletRequest) = false
            override fun fetchGuestToken(request: jakarta.servlet.http.HttpServletRequest): GuestAccess? = null
            override fun hash(request: jakarta.servlet.http.HttpServletRequest, guestAccess: GuestAccess) {}
            override fun genToken(
                request: jakarta.servlet.http.HttpServletRequest,
                response: jakarta.servlet.http.HttpServletResponse,
            ) = GuestAccess()
        }
        val store = object : IGuestAccessStore {
            override fun store(guestAccess: GuestAccess) {}
            override fun count() = error("not exercised")
            override fun getByHash(key: String) = error("not exercised")
        }

        val filter = config.guestAccessFilter(service, store)

        assertIs<GuestAccessFilter>(filter)
        // The filter must actually be wired to the supplied service instance.
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), org.springframework.mock.web.MockFilterChain())
        assertEquals(1, service.enabledCalls, "filter bean must consult the injected service")
    }

    @Test
    fun getComponentName_reportsModuleName() {
        assertEquals("kudos-ability-web-guest", config.getComponentName())
    }

    private fun <T : Any> emptyProvider(): ObjectProvider<T> = object : ObjectProvider<T> {
        override fun getObject(): T = throw UnsupportedOperationException("test stub: no beans")
        override fun getObject(vararg args: Any?): T = throw UnsupportedOperationException("test stub: no beans")
        override fun getIfAvailable(): T? = null
        override fun getIfUnique(): T? = null
        override fun stream(): Stream<T> = Stream.empty()
        override fun orderedStream(): Stream<T> = Stream.empty()
    }

    private fun <T : Any> providerOf(bean: T): ObjectProvider<T> = object : ObjectProvider<T> {
        override fun getObject(): T = bean
        override fun getObject(vararg args: Any?): T = bean
        override fun getIfAvailable(): T? = bean
        override fun getIfUnique(): T? = bean
        override fun stream(): Stream<T> = Stream.of(bean)
        override fun orderedStream(): Stream<T> = Stream.of(bean)
    }
}
