package io.kudos.ability.security.enforcement.point


/**
 * One permission point, as the owning module declares it.
 *
 * A point is the *existence* of a guarded action, not a grant of it. Registering every endpoint an
 * application exposes hands nobody any access — it only makes the actions nameable, which is the
 * precondition for granting or refusing them. That distinction is why registration is on by default
 * while enforcement is not.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class PermissionPointDefinition(

    /** The stable identity: `域:资源类型:动作`, e.g. `auth:group:bindUsers`. */
    val code: String,

    /** Human-readable name for the admin UI. */
    val name: String,

    /**
     * The endpoint this point guards, in the registry's URL syntax — either a bare path or a
     * method-qualified `POST:/api/...`. Null for points with no HTTP surface (a scheduled job, a
     * message consumer, a UI-only action), which are reachable only through `@RequiresPermission`.
     */
    val url: String? = null,

    /** Subsystem the point belongs to; scopes the code's uniqueness. */
    val subsystem: String = DEFAULT_SUBSYSTEM,

    /** Free-text note stored alongside the row. */
    val remark: String? = null,

    /**
     * True when [code] was derived from the URL by convention rather than declared explicitly.
     *
     * The distinction matters on **re-registration**: a derived code follows the path, so renaming
     * an endpoint mints a new code and silently orphans every grant of the old one. A declared code
     * (via `@RequiresPermission`) is pinned and survives the rename. The store records this so an
     * operator can see which of their points are load-bearing by accident.
     */
    val derived: Boolean = false,
) {
    companion object {
        const val DEFAULT_SUBSYSTEM = "default-sub-system"
    }
}


/**
 * Contributes permission points that no URL scan can find.
 *
 * The scanner covers HTTP endpoints, which is most of an application's guarded surface but not all
 * of it: a scheduled job, a message consumer, an internal service call or a purely client-side
 * action has no request to match on. A module declares those here, and they become grantable in
 * exactly the same way.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IPermissionPointContributor {

    /** The points this module owns. Called once during startup. */
    fun points(): Collection<PermissionPointDefinition>
}


/**
 * Turns an HTTP endpoint into a permission code.
 *
 * Separated from the scanner so an application whose paths do not follow kudos' convention can
 * supply its own rule without reimplementing the scan. Returning null means "this endpoint gets no
 * derived code" — it is then registrable only by annotating it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IPermissionPointNaming {

    /**
     * @param httpMethod HTTP method, upper case
     * @param pathPattern the mapped path pattern, e.g. `/api/admin/auth/group/bindUsers`
     * @return the derived code, or null when this endpoint should not get one
     */
    fun codeFor(httpMethod: String, pathPattern: String): String?
}


/**
 * Where registered points are persisted.
 *
 * A port rather than a direct dependency for the same reason as the decision ports: this module
 * must not know which microservice owns the registry table, and an application with its own
 * registry implements this instead.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IPermissionPointStore {

    /**
     * Upserts [points], keyed by `(code, subsystem)`.
     *
     * Must be idempotent: this runs on every boot of every instance, so a second call with the same
     * input has to be a no-op rather than a duplicate. Must never delete: a point that has
     * disappeared from the code may still carry grants, and silently dropping it would turn a
     * refactor into an invisible permission change.
     *
     * @param points the declared points
     * @return how many rows were created or updated
     */
    fun register(points: Collection<PermissionPointDefinition>): Int
}
