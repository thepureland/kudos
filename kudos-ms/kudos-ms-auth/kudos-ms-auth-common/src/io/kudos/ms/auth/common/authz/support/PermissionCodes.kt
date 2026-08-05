package io.kudos.ms.auth.common.authz.support


/**
 * Matching rules for semantic permission codes (`域:资源类型:动作`, e.g. `sys:user:delete`).
 *
 * A code is a colon-separated list of segments. A **grant** may use `*` as a segment:
 *
 * | grant | matches | does not match |
 * |---|---|---|
 * | `sys:user:delete` | `sys:user:delete` | `sys:user:read` |
 * | `sys:user:*` | `sys:user:delete`, `sys:user:read` | `sys:role:delete` |
 * | `sys:*:read` | `sys:user:read`, `sys:role:read` | `sys:user:delete` |
 * | `sys:*` | `sys:user`, `sys:user:delete` (whole subtree) | `msg:user:delete` |
 * | `*` | everything | — |
 *
 * The trailing-`*` rule is what makes "grant the entire module" a single row instead of an
 * enumeration that silently goes stale every time a permission point is added. A `*` in any other
 * position matches exactly one segment, so `sys:*:read` cannot accidentally swallow deeper codes.
 *
 * Requests are always concrete: a `*` in a request is treated as a literal segment and will only
 * match a grant that also has `*` there. Asking "may I do anything under sys?" is not a question the
 * decision point answers.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object PermissionCodes {

    /** Segment separator. */
    const val SEPARATOR = ":"

    /** Wildcard segment. */
    const val WILDCARD = "*"

    /**
     * Whether [grantCode] covers [requestedCode].
     *
     * @param grantCode the code stored on a permission binding; may contain `*` segments
     * @param requestedCode the concrete code being asked about
     * @return true when the grant covers the request
     */
    @JvmStatic
    fun matches(grantCode: String?, requestedCode: String?): Boolean {
        val grant = grantCode?.trim().orEmpty()
        val requested = requestedCode?.trim().orEmpty()
        if (grant.isEmpty() || requested.isEmpty()) return false
        if (grant == requested) return true

        val grantSegments = grant.split(SEPARATOR)
        val requestSegments = requested.split(SEPARATOR)

        // A trailing wildcard covers the whole subtree, so the grant may be shorter than the request.
        val trailingWildcard = grantSegments.last() == WILDCARD
        if (grantSegments.size > requestSegments.size) return false
        if (grantSegments.size < requestSegments.size && !trailingWildcard) return false

        for ((index, grantSegment) in grantSegments.withIndex()) {
            if (grantSegment == WILDCARD) {
                // The trailing wildcard has already absorbed whatever remains.
                if (index == grantSegments.lastIndex) return true
                continue
            }
            if (grantSegment != requestSegments[index]) return false
        }
        return true
    }

    /**
     * Whether any of [grantCodes] covers [requestedCode].
     *
     * @param grantCodes the codes held by the subject
     * @param requestedCode the concrete code being asked about
     * @return true when at least one grant covers the request
     */
    @JvmStatic
    fun anyMatches(grantCodes: Iterable<String>, requestedCode: String?): Boolean =
        grantCodes.any { matches(it, requestedCode) }
}
