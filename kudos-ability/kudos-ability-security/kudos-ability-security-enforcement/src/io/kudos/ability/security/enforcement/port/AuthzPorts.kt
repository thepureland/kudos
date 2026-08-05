package io.kudos.ability.security.enforcement.port


/**
 * What the enforcement layer needs from a decision point, expressed so that this module never
 * depends on the one that answers.
 *
 * The dependency runs the other way on purpose: `kudos-ms-auth` implements these, and an application
 * with a different authorization backend implements them instead — the filter and the aspect stay
 * the same. It also keeps the ability layer free of any microservice dependency, which the build
 * would otherwise have to allow.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthzDecisionProvider {

    /**
     * Whether the current caller may exercise [permissionCode].
     *
     * Implementations resolve the caller themselves (from the security context / login state) — the
     * enforcement points deliberately do not pass a subject, so there is no way for a caller to
     * ask on somebody else's behalf.
     *
     * @param permissionCode the concrete `域:资源类型:动作` code required
     * @param context request attributes available to conditional bindings (client ip, …)
     * @return true when the action is permitted
     */
    fun isPermitted(permissionCode: String, context: Map<String, Any?> = emptyMap()): Boolean

    /**
     * Whether there is an authenticated caller at all. A request with no subject can never be
     * permitted, and the enforcement points report it differently (401 rather than 403).
     */
    fun hasSubject(): Boolean
}


/**
 * The registry of permission points, as far as URL-based enforcement is concerned.
 *
 * A path that resolves to no code is **not** an unprotected path — it is an unregistered one, and
 * the filter refuses it (see `PermissionEnforcementFilter`). Public endpoints are declared as such
 * in configuration, never by omission.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IPermissionPointRegistry {

    /**
     * The permission code guarding an HTTP endpoint.
     *
     * @param method HTTP method, upper case
     * @param path request path, without query string or context path
     * @return the code to check, or null when no permission point is registered for this endpoint
     */
    fun resolve(method: String, path: String): String?
}
