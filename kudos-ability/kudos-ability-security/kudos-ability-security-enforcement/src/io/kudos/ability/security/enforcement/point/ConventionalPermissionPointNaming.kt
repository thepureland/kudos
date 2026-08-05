package io.kudos.ability.security.enforcement.point


/**
 * The default URL→code rule, matching kudos' own path convention.
 *
 * ```
 * /api/admin/auth/group/bindUsers   →  auth:group:bindUsers
 * /api/admin/sys/dict/save          →  sys:dict:save
 * /api/internal/user/account/getById→  user:account:getById
 * /api/admin/auth/role/{id}/detail  →  auth:role:*:detail
 * ```
 *
 * **Why derive at all rather than make every module declare its points.** Declaring them is the
 * honest thing for a handful of points and unworkable for a few hundred: kudos' own admin surface
 * alone is that size, and a registry nobody can populate is a registry nobody turns on. Derivation
 * makes the default state of a kudos application *fully registered*, which is the state URL
 * enforcement needs before it can be switched on at all.
 *
 * **What derivation costs, stated plainly.** A derived code follows the path. Rename the endpoint
 * and the code changes with it, orphaning existing grants — the grants do not break loudly, they
 * simply stop matching. So a code that has been granted in anger should be **pinned** with
 * `@RequiresPermission`, after which the path is free to move. The registry marks derived rows so
 * this is visible rather than folklore.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class ConventionalPermissionPointNaming(

    /**
     * Path prefixes stripped before the code is read off. Each is an `/api/{scope}/`-style prefix;
     * the scope segment (`admin`, `public`, `internal`) is *not* part of the code, because the same
     * action reached through two surfaces is one permission, not two.
     */
    private val strippedPrefixes: List<String> = DEFAULT_STRIPPED_PREFIXES,
) : IPermissionPointNaming {

    override fun codeFor(httpMethod: String, pathPattern: String): String? {
        val prefix = strippedPrefixes.firstOrNull { pathPattern.startsWith(it) } ?: return null
        val tail = pathPattern.removePrefix(prefix).trim('/')
        if (tail.isEmpty()) return null

        val segments = tail.split('/')
            .filter { it.isNotBlank() }
            // A path variable identifies *which* row, never *what may be done to it*, so it carries
            // no meaning in a code. Collapsing it to a wildcard segment keeps `/role/{id}/detail`
            // and `/role/{roleId}/detail` from minting two codes for one action.
            .map { if (it.startsWith("{") && it.endsWith("}")) "*" else it }
        // Fewer than three segments is not a shape this convention can name honestly: there is no
        // action segment to read, and inventing one (`list`, `index`) would guess at semantics.
        if (segments.size < 3) return null
        return segments.joinToString(":")
    }

    companion object {
        val DEFAULT_STRIPPED_PREFIXES: List<String> = listOf(
            "/api/admin/",
            "/api/internal/",
            "/api/public/",
            "/api/",
        )
    }
}
