package io.kudos.ability.web.guest.filter

import io.kudos.ability.web.guest.provider.GuestAccess
import io.kudos.ability.web.guest.provider.IGuestAccessService
import io.kudos.ability.web.guest.provider.IGuestAccessStore
import io.kudos.ability.web.guest.provider.IGuestAccessUniqueKey
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [GuestAccessFilter] using hand-rolled fake collaborators.
 *
 * The filter delegates real work to [IGuestAccessService] + [IGuestAccessStore]; this test cares
 * about the routing logic:
 *  - disabled → no SPI call;
 *  - any ignore returns true → no SPI call;
 *  - fetchGuestToken returns null → genToken called, store NOT called (first-visit path);
 *  - fetchGuestToken returns a populated visitor → hash + store called, genToken NOT called
 *    (returning-visitor path);
 *  - any exception from the SPI must be swallowed, not propagated to the chain.
 *
 * In every case the filter chain MUST advance — guest tracking is a sidecar; it must never
 * short-circuit the real request.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class GuestAccessFilterTest {

    private val service = RecordingService()
    private val store = RecordingStore()
    private val filter = GuestAccessFilter(service, store)

    @Test
    fun disabled_bypassesAllSpiCalls_chainStillAdvances() {
        service.enabled = false
        val chain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertEquals(0, service.fetchCalls)
        assertEquals(0, service.hashCalls)
        assertEquals(0, service.genCalls)
        assertEquals(0, store.storeCalls)
        assertTrue(chain.request != null, "filter chain must have been advanced even when disabled")
    }

    @Test
    fun ignored_bypassesAllSpiCalls_chainStillAdvances() {
        service.enabled = true
        service.ignoreResult = true
        val chain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertEquals(0, service.fetchCalls, "ignore=true must short-circuit before fetchGuestToken")
        assertEquals(0, store.storeCalls)
        assertTrue(chain.request != null)
    }

    @Test
    fun noExistingToken_callsGenToken_notStore() {
        service.enabled = true
        service.ignoreResult = false
        service.fetchResult = null
        val chain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertEquals(1, service.fetchCalls)
        assertEquals(1, service.genCalls, "first-visit path must mint a fresh token")
        assertEquals(0, service.hashCalls, "first-visit path must NOT compute a hash")
        assertEquals(0, store.storeCalls, "first-visit path must NOT touch the store")
    }

    @Test
    fun existingToken_callsHashAndStore_notGen() {
        service.enabled = true
        service.ignoreResult = false
        service.fetchResult = GuestAccess().apply { token = "some-encrypted-token" }
        val chain = MockFilterChain()

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertEquals(1, service.fetchCalls)
        assertEquals(1, service.hashCalls, "returning visitor must populate the hash")
        assertEquals(1, store.storeCalls, "returning visitor must be persisted")
        assertEquals(0, service.genCalls, "returning visitor must NOT mint a new cookie")
    }

    @Test
    fun blankTokenInExistingGuestAccess_routesToGenTokenPath() {
        // Defensive case: a fetched GuestAccess whose `token` is blank/null is treated as "no
        // token present" — same as fetchResult == null — so a tampered-but-decoded cookie can't
        // sneak past as a returning visitor.
        service.enabled = true
        service.ignoreResult = false
        service.fetchResult = GuestAccess().apply { token = "" }

        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), MockFilterChain())

        assertEquals(1, service.genCalls)
        assertEquals(0, store.storeCalls)
    }

    @Test
    fun spiException_isSwallowed_chainStillAdvances() {
        service.enabled = true
        service.ignoreResult = false
        service.fetchThrows = RuntimeException("simulated store outage")
        val chain = MockFilterChain()

        // The filter MUST NOT propagate — visitor tracking is a sidecar concern.
        filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), chain)

        assertTrue(chain.request != null, "filter chain must still advance even when the SPI throws")
    }

    private class RecordingService : IGuestAccessService {
        var enabled = true
        var ignoreResult = false
        var fetchResult: GuestAccess? = null
        var fetchThrows: RuntimeException? = null
        var fetchCalls = 0
        var hashCalls = 0
        var genCalls = 0

        override fun isEnabled() = enabled
        override fun isExclude(request: HttpServletRequest) = ignoreResult
        override fun fetchGuestToken(request: HttpServletRequest): GuestAccess? {
            fetchCalls++
            fetchThrows?.let { throw it }
            return fetchResult
        }

        override fun hash(request: HttpServletRequest, guestAccess: GuestAccess) {
            hashCalls++
            guestAccess.hash = "computed-hash"
        }

        override fun genToken(request: HttpServletRequest, response: HttpServletResponse): GuestAccess {
            genCalls++
            return GuestAccess().apply { token = "newly-minted" }
        }
    }

    private class RecordingStore : IGuestAccessStore {
        var storeCalls = 0
        override fun store(guestAccess: GuestAccess) {
            storeCalls++
        }
        override fun count() = error("test fixture: count() not exercised by these scenarios")
        override fun getByHash(key: String) = error("test fixture: getByHash() not exercised")
    }

    @Suppress("unused")
    private val unusedUniqueKey: IGuestAccessUniqueKey = IGuestAccessUniqueKey { "ignored" }
}
