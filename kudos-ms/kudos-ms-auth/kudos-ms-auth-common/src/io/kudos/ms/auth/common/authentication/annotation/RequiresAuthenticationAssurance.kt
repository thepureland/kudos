package io.kudos.ms.auth.common.authentication.annotation

/**
 * Requires an authenticated session whose ACR reaches [acr] before the annotated method runs.
 *
 * Set [maxAgeSeconds] when the operation also requires recent authentication. Its default disables
 * the freshness check, so ACR-only declarations do not unexpectedly force periodic reauthentication.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequiresAuthenticationAssurance(

    /** Protocol-neutral authentication context class reference required by this operation. */
    val acr: String,

    /** Maximum age of auth_time in seconds, or [NO_MAX_AGE] to accept any active session age. */
    val maxAgeSeconds: Long = NO_MAX_AGE,
) {
    companion object {
        const val NO_MAX_AGE = -1L
    }
}
