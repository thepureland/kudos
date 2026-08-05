package io.kudos.ms.auth.core.platform.authz.enforcement

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * junit test for [PermissionPointRegistry].
 *
 * Test data source: `PermissionPointRegistryTest.sql`
 *
 * This class resolves an HTTP request to the permission code guarding it, which makes every rule
 * here a security rule. The one worth stating outright: **the answer must not depend on row order**.
 * A registry whose decision changes when somebody re-seeds the table is not something anyone can
 * reason about, so the fixture inserts the rows in the reverse of the expected precedence.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class PermissionPointRegistryTest : RdbTestBase() {

    @Resource
    private lateinit var registry: PermissionPointRegistry

    private fun resolve(method: String, path: String): String? {
        // The snapshot is TTL'd; the fixture landed after the context came up.
        registry.invalidate()
        return registry.resolve(method, path)
    }

    /**
     * A method-qualified point is the more specific statement, so it wins — otherwise registering
     * `POST:/x` alongside an existing `/x` would silently do nothing.
     */
    @Test
    fun methodQualifiedBeatsMethodAgnostic() {
        assertEquals("ppr:role:bindUsers", resolve("POST", "/api/admin/ppr/role/bindUsers"))
    }

    /** The unqualified row still covers the verbs the qualified one does not claim. */
    @Test
    fun theMethodAgnosticRowStillCoversOtherVerbs() {
        assertEquals("ppr:role:bindUsers:any", resolve("GET", "/api/admin/ppr/role/bindUsers"))
    }

    /**
     * Among rows of equal qualification the longest pattern wins: an exact path is a more precise
     * statement than the subtree wildcard it sits inside.
     */
    @Test
    fun longestPatternWinsAmongEqualCandidates() {
        assertEquals("ppr:group:listUserIds", resolve("GET", "/api/admin/ppr/group/listUserIds"))
    }

    /** …and the subtree row still guards everything the exact rows do not name. */
    @Test
    fun theSubtreeRowCoversTheRest() {
        assertEquals("ppr:group:subtree", resolve("GET", "/api/admin/ppr/group/somethingElse"))
    }

    /**
     * An unregistered path resolves to nothing. That is *not* the same as unprotected — the filter
     * refuses it — but the registry's own contract is simply "no code here".
     */
    @Test
    fun anUnregisteredPathResolvesToNothing() {
        assertNull(resolve("GET", "/api/admin/ppr/never/registered"))
    }

    /** Deactivating a permission point takes it out of resolution. */
    @Test
    fun aDeactivatedPointIsNotResolved() {
        assertNull(resolve("GET", "/api/admin/ppr/dead/endpoint"))
    }

    /**
     * A row with a url but no permission code is the normal intermediate state of the migration to
     * codes. It is not enforceable, and pretending otherwise would deny an endpoint whose
     * permission nobody has named yet.
     */
    @Test
    fun aRowWithoutAPermissionCodeIsNotAPermissionPoint() {
        assertNull(resolve("GET", "/api/admin/ppr/uncoded/thing"))
    }

    /**
     * Only a leading token that really is an HTTP method qualifies the path. A colon elsewhere is
     * part of the path, and reading it as a method would leave the endpoint permanently unmatchable.
     */
    @Test
    fun aColonInsideThePathIsNotAMethodQualifier() {
        assertEquals("ppr:odd:colonPath", resolve("GET", "/api/admin/ppr/odd/we:ird"))
    }

    /** Method matching is case-insensitive; containers and clients are not consistent about it. */
    @Test
    fun methodMatchingIsCaseInsensitive() {
        assertEquals("ppr:role:bindUsers", resolve("post", "/api/admin/ppr/role/bindUsers"))
    }
}
