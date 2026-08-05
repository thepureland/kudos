package io.kudos.ability.security.enforcement.annotation


/**
 * Requires the caller to hold [value] before the annotated method runs.
 *
 * The URL filter covers HTTP endpoints, which is most of the surface but not all of it: message
 * consumers, scheduled jobs, internal services and any method reached other than through a request
 * have no path to match on. This is the same enforcement for those.
 *
 * ```kotlin
 * @RequiresPermission("sys:user:delete")
 * fun deleteUser(id: String) { … }
 * ```
 *
 * Wildcards are meaningless here — a *requirement* is always a concrete code; only a *grant* may be
 * written with `*`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequiresPermission(

    /** The concrete permission code the caller must hold. */
    val value: String,
)
