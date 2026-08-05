package io.kudos.ability.security.enforcement.point

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull


/**
 * junit test for [ConventionalPermissionPointNaming].
 *
 * This rule decides what every auto-registered permission is *called*, and a code is the durable
 * identity grants are written against. So the cases that matter are the ones where a plausible
 * implementation would quietly produce two names for one action, or one name for two.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class ConventionalPermissionPointNamingTest {

    private val naming = ConventionalPermissionPointNaming()

    @Test
    fun readsDomainResourceActionOffThePath() {
        assertEquals("auth:group:bindUsers", naming.codeFor("POST", "/api/admin/auth/group/bindUsers"))
        assertEquals("sys:dict:save", naming.codeFor("POST", "/api/admin/sys/dict/save"))
        assertEquals("user:account:getById", naming.codeFor("GET", "/api/internal/user/account/getById"))
    }

    /**
     * The same action reached through the admin and the internal surface is **one** permission.
     * Including the scope segment would mint `admin:auth:group` and `internal:auth:group`, and an
     * administrator granting the first would be surprised by the second.
     */
    @Test
    fun theApiScopeSegmentIsNotPartOfTheCode() {
        assertEquals(
            naming.codeFor("POST", "/api/admin/auth/role/save"),
            naming.codeFor("POST", "/api/internal/auth/role/save"),
        )
    }

    /**
     * A path variable says *which* row, never *what may be done to it*. Two spellings of the same
     * variable must not become two permissions.
     */
    @Test
    fun pathVariablesCollapseToAWildcardSegment() {
        assertEquals("auth:role:*:detail", naming.codeFor("GET", "/api/admin/auth/role/{id}/detail"))
        assertEquals(
            naming.codeFor("GET", "/api/admin/auth/role/{id}/detail"),
            naming.codeFor("GET", "/api/admin/auth/role/{roleId}/detail"),
        )
    }

    /**
     * A shape with no action segment cannot be named honestly. Inventing one would guess at
     * semantics the path never stated — and a wrong guess here is a permission code somebody grants
     * believing it means something else.
     */
    @Test
    fun refusesToNameShapesItCannotRead() {
        assertNull(naming.codeFor("GET", "/api/admin/auth/group"), "no action segment")
        assertNull(naming.codeFor("GET", "/api/admin"), "nothing after the scope")
        assertNull(naming.codeFor("GET", "/login"), "outside the api namespace")
        assertNull(naming.codeFor("GET", "/actuator/health"))
    }

    @Test
    fun toleratesTrailingAndDoubledSlashes() {
        assertEquals("auth:group:bindUsers", naming.codeFor("POST", "/api/admin/auth/group/bindUsers/"))
        assertEquals("auth:group:bindUsers", naming.codeFor("POST", "/api/admin/auth//group//bindUsers"))
    }

    /** Deeper paths keep every segment; the code stays segment-addressable for wildcard grants. */
    @Test
    fun deeperPathsKeepEverySegment() {
        assertEquals(
            "auth:role:scope:region:bind",
            naming.codeFor("POST", "/api/admin/auth/role/scope/region/bind"),
        )
    }
}
