package io.kudos.ability.security.enforcement.filter

import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator
import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator.Freshness
import io.kudos.ability.security.enforcement.port.ITrustedAuthorizationContextProvider
import jakarta.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


/**
 * junit test for [PermissionEnforcementFilter].
 *
 * The behaviour under test is mostly about what happens in the *absence* of information: an
 * unregistered path, an unauthenticated caller, a subject without the permission. Those are the
 * cases where a filter is tempted to be lenient, and where leniency is the vulnerability.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
class PermissionEnforcementFilterTest {

    private class FakeProvider(
        var subjectPresent: Boolean = true,
        var permitted: Boolean = true,
    ) : IAuthzDecisionProvider {
        var lastCode: String? = null
        var lastContext: Map<String, Any?> = emptyMap()
        override fun isPermitted(permissionCode: String, context: Map<String, Any?>): Boolean {
            lastCode = permissionCode
            lastContext = context
            return permitted
        }
        override fun hasSubject(): Boolean = subjectPresent
    }

    private val registry = object : IPermissionPointRegistry {
        override fun resolve(method: String, path: String): String? =
            if (method == "POST" && path == BOUND_PATH) "auth:role:bind" else null
    }

    private fun filter(
        provider: IAuthzDecisionProvider,
        properties: EnforcementProperties = EnforcementProperties().apply { shadowMode = false },
        freshness: ITokenFreshnessValidator? = null,
        trustedContextProviders: List<ITrustedAuthorizationContextProvider> = emptyList(),
    ) = PermissionEnforcementFilter(provider, registry, properties, freshness, trustedContextProviders)

    private fun freshnessOf(value: Freshness) = object : ITokenFreshnessValidator {
        override fun assess(): Freshness = value
    }

    private fun enforcing(configure: EnforcementProperties.() -> Unit = {}) =
        EnforcementProperties().apply { shadowMode = false }.apply(configure)

    private fun request(
        method: String = "POST",
        path: String = BOUND_PATH,
        remoteAddr: String = "10.1.2.3",
        headers: Map<String, String> = emptyMap(),
    ) = MockHttpServletRequest(method, path).apply {
        servletPath = path
        this.remoteAddr = remoteAddr
        headers.forEach { (name, value) -> addHeader(name, value) }
    }

    @Test
    fun permittedRequestPassesThrough() {
        val chain = MockFilterChain()
        val provider = FakeProvider(permitted = true)
        filter(provider).doFilter(request(), MockHttpServletResponse(), chain)
        assertNotNull(chain.request, "the request must reach the application")
        assertEquals("auth:role:bind", provider.lastCode)
    }

    @Test
    fun subjectWithoutThePermissionGets403() {
        val chain = MockFilterChain()
        val response = MockHttpServletResponse()
        filter(FakeProvider(permitted = false)).doFilter(request(), response, chain)
        assertNull(chain.request, "the request must not reach the application")
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status)
        assertTrue(response.contentAsString.contains("auth:role:bind"), "the body should name the missing permission")
    }

    @Test
    fun anonymousCallerGets401NotJust403() {
        // "I don't know who you are" and "I know who you are and you may not" are different
        // situations; a client can only react correctly if the filter distinguishes them.
        val chain = MockFilterChain()
        val response = MockHttpServletResponse()
        filter(FakeProvider(subjectPresent = false)).doFilter(request(), response, chain)
        assertNull(chain.request)
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun unregisteredPathIsRefused() {
        // The load-bearing decision: an endpoint nobody registered is closed, not open — otherwise
        // every forgotten registration becomes a silent hole.
        val chain = MockFilterChain()
        val response = MockHttpServletResponse()
        filter(FakeProvider()).doFilter(request(path = "/api/admin/something/unregistered"), response, chain)
        assertNull(chain.request)
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status)
    }

    @Test
    fun aRegisteredPathIsStillRefusedForTheWrongMethod() {
        val chain = MockFilterChain()
        val response = MockHttpServletResponse()
        filter(FakeProvider()).doFilter(request(method = "DELETE"), response, chain)
        assertNull(chain.request)
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status)
    }

    @Test
    fun publicPathsAreDeclaredNotInferred() {
        val properties = EnforcementProperties().apply {
            shadowMode = false
            publicPaths = listOf("/api/public/" + "**")
        }
        val chain = MockFilterChain()
        filter(FakeProvider(subjectPresent = false), properties)
            .doFilter(request(path = "/api/public/login"), MockHttpServletResponse(), chain)
        assertNotNull(chain.request, "a declared public path needs no subject and no permission")
    }

    @Test
    fun shadowModeLetsEverythingThroughButRecordsIt() {
        // The migration path: an existing deployment discovers its gaps from real traffic instead of
        // from an outage.
        val properties = EnforcementProperties().apply { shadowMode = true }
        val cases = listOf(
            FakeProvider(permitted = false) to request(),
            FakeProvider(subjectPresent = false) to request(),
            FakeProvider() to request(path = "/api/admin/unregistered"),
        )
        for ((provider, req) in cases) {
            val chain = MockFilterChain()
            val response = MockHttpServletResponse()
            filter(provider, properties).doFilter(req, response, chain)
            assertNotNull(chain.request, "shadow mode must not block")
            assertEquals(200, response.status, "shadow mode must not rewrite the status")
        }
    }

    @Test
    fun clientIpReachesTheConditionContext() {
        val provider = FakeProvider()
        filter(provider).doFilter(request(remoteAddr = "10.9.9.9"), MockHttpServletResponse(), MockFilterChain())
        assertEquals("10.9.9.9", provider.lastContext["ip"])

        // A proxied deployment is the norm; the first hop of X-Forwarded-For is the client.
        filter(provider, enforcing { trustForwardedFor = true }).doFilter(
            request(headers = mapOf("X-Forwarded-For" to "203.0.113.7, 10.0.0.1")),
            MockHttpServletResponse(),
            MockFilterChain(),
        )
        assertEquals("203.0.113.7", provider.lastContext["ip"])
    }

    @Test
    fun configuredHeadersAreLiftedIntoTheContext() {
        val properties = EnforcementProperties().apply {
            shadowMode = false
            contextHeaders = listOf("X-Tenant")
            acceptUntrustedContextHeaders = true
        }
        val provider = FakeProvider()
        filter(provider, properties).doFilter(
            request(headers = mapOf("X-Tenant" to "acme")),
            MockHttpServletResponse(),
            MockFilterChain(),
        )
        assertEquals("acme", provider.lastContext["X-Tenant"])
    }

    @Test
    fun callerHeadersAreIgnoredByDefaultButTrustedProvidersCanSupplyAttributes() {
        val provider = FakeProvider()
        val properties = enforcing { contextHeaders = listOf("department") }
        val trusted = ITrustedAuthorizationContextProvider { mapOf("department" to "finance") }
        filter(provider, properties, trustedContextProviders = listOf(trusted)).doFilter(
            request(headers = mapOf("department" to "attacker-value")),
            MockHttpServletResponse(),
            MockFilterChain(),
        )
        assertEquals("finance", provider.lastContext["department"])
    }

    companion object {
        private const val BOUND_PATH = "/api/admin/auth/role/bindUsers"
    }


    // ---- Credential freshness ----

    /**
     * A stale credential is a 401, not a 403: the caller may well be entitled to the action, but the
     * thing they presented predates a change and the remedy is to re-authenticate, not to ask for
     * more permission.
     */
    @Test
    fun aStaleCredentialGets401() {
        val chain = MockFilterChain()
        val response = MockHttpServletResponse()
        val properties = enforcing { tokenFreshness.enabled = true }
        filter(FakeProvider(permitted = true), properties, freshnessOf(Freshness.STALE))
            .doFilter(request(), response, chain)
        assertNull(chain.request, "a request on a stale credential must not reach the application")
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
    }

    /**
     * Checked *before* the decision. A token minted before a revocation still names a subject the
     * decision point will happily answer for, so asking "may they" first would answer a question
     * about a credential nobody should still be honouring.
     */
    @Test
    fun freshnessIsCheckedBeforeTheDecisionPointIsConsulted() {
        val provider = FakeProvider(permitted = true)
        val properties = enforcing { tokenFreshness.enabled = true }
        filter(provider, properties, freshnessOf(Freshness.STALE))
            .doFilter(request(), MockHttpServletResponse(), MockFilterChain())
        assertNull(provider.lastCode, "the decision point must not have been asked at all")
    }

    @Test
    fun aFreshCredentialPassesThrough() {
        val chain = MockFilterChain()
        val properties = enforcing { tokenFreshness.enabled = true }
        filter(FakeProvider(permitted = true), properties, freshnessOf(Freshness.FRESH))
            .doFilter(request(), MockHttpServletResponse(), chain)
        assertNotNull(chain.request)
    }

    /**
     * The migration default. Every token minted before this feature existed is unversioned, so
     * refusing them the moment the check is switched on is a mass logout — the exact flag-day the
     * rest of this module is built to avoid.
     */
    @Test
    fun anUnversionedCredentialIsAllowedByDefault() {
        val chain = MockFilterChain()
        val properties = enforcing { tokenFreshness.enabled = true }
        filter(FakeProvider(permitted = true), properties, freshnessOf(Freshness.UNKNOWN))
            .doFilter(request(), MockHttpServletResponse(), chain)
        assertNotNull(chain.request, "unversioned credentials must not break on day one")
    }

    /** ...and is refused once the deployment says its issuer stamps versions. */
    @Test
    fun anUnversionedCredentialIsRefusedOnceTightened() {
        val chain = MockFilterChain()
        val response = MockHttpServletResponse()
        val properties = enforcing {
            tokenFreshness.enabled = true
            tokenFreshness.rejectUnversionedTokens = true
        }
        filter(FakeProvider(permitted = true), properties, freshnessOf(Freshness.UNKNOWN))
            .doFilter(request(), response, chain)
        assertNull(chain.request)
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
    }

    /** Adding the port to a build must change nothing until somebody opts in. */
    @Test
    fun freshnessIsInertWhileSwitchedOff() {
        val chain = MockFilterChain()
        filter(FakeProvider(permitted = true), enforcing(), freshnessOf(Freshness.STALE))
            .doFilter(request(), MockHttpServletResponse(), chain)
        assertNotNull(chain.request, "the check is off, so a stale verdict must be ignored")
    }

    /** No implementation on the classpath is simply an absent check, not a startup failure. */
    @Test
    fun anAbsentValidatorLeavesTheFilterWorking() {
        val chain = MockFilterChain()
        val properties = enforcing { tokenFreshness.enabled = true }
        filter(FakeProvider(permitted = true), properties, null)
            .doFilter(request(), MockHttpServletResponse(), chain)
        assertNotNull(chain.request)
    }

    /** Shadow mode covers this refusal too — the migration log-first path is the same one. */
    @Test
    fun aStaleCredentialIsOnlyLoggedInShadowMode() {
        val chain = MockFilterChain()
        val properties = EnforcementProperties().apply {
            shadowMode = true
            tokenFreshness.enabled = true
        }
        filter(FakeProvider(permitted = true), properties, freshnessOf(Freshness.STALE))
            .doFilter(request(), MockHttpServletResponse(), chain)
        assertNotNull(chain.request)
    }

    // ---- Path resolution: the filter must judge the string the router will judge ----

    /**
     * A context path must not make every endpoint look unregistered.
     *
     * `requestURI` carries it; the registered patterns do not. Resolving the path the way Spring
     * routes it strips the context path, so the two sides compare like for like.
     */
    @Test
    fun aContextPathDoesNotHideTheRegisteredPoint() {
        val provider = FakeProvider(permitted = true)
        val request = MockHttpServletRequest("POST", "/myapp${BOUND_PATH}").apply {
            contextPath = "/myapp"
            servletPath = ""
            requestURI = "/myapp${BOUND_PATH}"
        }
        val chain = MockFilterChain()
        filter(provider).doFilter(request, MockHttpServletResponse(), chain)
        assertEquals("auth:role:bind", provider.lastCode, "the point must resolve despite the context path")
        assertNotNull(chain.request)
    }

    /**
     * The one that could fail *open*: a raw path shaped to match a public prefix.
     *
     * Matching public paths against an undecoded URI lets an encoded traversal out of the actuator
     * prefix satisfy that prefix pattern, while the container decodes it into something else
     * entirely. Whether it then reaches a protected handler depends on the container — Tomcat
     * rejects these outright today, which is the container's choice and not this filter's
     * guarantee. Resolving through the same helper Spring routes with removes the disagreement
     * rather than relying on the accident.
     */
    @Test
    fun anEncodedTraversalCannotMasqueradeAsAPublicPath() {
        val properties = EnforcementProperties().apply {
            shadowMode = false
            publicPaths = listOf("/actuator/" + "**")
        }
        val provider = FakeProvider(permitted = true)
        val request = MockHttpServletRequest("POST", BOUND_PATH).apply {
            servletPath = ""
            // What an attacker puts on the wire; the container decodes this to the bound path.
            requestURI = "/actuator/..%2f" + BOUND_PATH.trimStart('/')
        }
        val chain = MockFilterChain()
        val response = MockHttpServletResponse()
        filter(provider, properties).doFilter(request, response, chain)

        // Whatever it resolves to, it must not be waved through as public without a decision.
        assertTrue(
            provider.lastCode != null || response.status != HttpServletResponse.SC_OK,
            "an encoded traversal must not be treated as a public path",
        )
    }

    /** Semicolon path parameters are routing noise, not part of the permission point. */
    @Test
    fun semicolonPathParametersDoNotHideThePoint() {
        val provider = FakeProvider(permitted = true)
        val request = MockHttpServletRequest("POST", BOUND_PATH).apply {
            servletPath = ""
            requestURI = "${BOUND_PATH};jsessionid=abc123"
        }
        filter(provider).doFilter(request, MockHttpServletResponse(), MockFilterChain())
        assertEquals("auth:role:bind", provider.lastCode)
    }
}
