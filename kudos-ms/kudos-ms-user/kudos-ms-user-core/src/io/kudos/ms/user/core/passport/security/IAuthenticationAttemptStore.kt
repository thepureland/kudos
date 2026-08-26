package io.kudos.ms.user.core.passport.security

data class AuthenticationAttemptBucket(
    val key: String,
    val maxAttempts: Int,
    val windowSeconds: Long,
)

data class AuthenticationAttemptStoreDecision(
    val allowed: Boolean,
    val retryAfterSeconds: Long? = null,
) {
    companion object {
        val ALLOWED = AuthenticationAttemptStoreDecision(allowed = true)
    }
}

/** Atomic fixed-window counter storage for one or more authentication dimensions. */
interface IAuthenticationAttemptStore {
    fun inspect(buckets: List<AuthenticationAttemptBucket>): AuthenticationAttemptStoreDecision

    fun consume(buckets: List<AuthenticationAttemptBucket>): AuthenticationAttemptStoreDecision

    fun clear(keys: Collection<String>)
}
