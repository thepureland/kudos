package io.kudos.ms.user.core.account.security

/**
 * Optional in-process bridge to a password-history owner such as `kudos-ms-auth`.
 *
 * Raw candidates are used only for one-way hash comparison and must never be persisted or logged.
 * Implementations should fail closed when their backing store is unavailable. An empty implementation
 * list intentionally keeps `kudos-ms-user-core` independently deployable during migration.
 */
interface IPasswordHistory {

    /** Whether [password] matches a retained hash for the account and password purpose. */
    fun isReused(password: String, context: PasswordPolicyContext): Boolean

    /** Records the password hash being replaced; [encodedPassword] is never a raw credential. */
    fun record(encodedPassword: String, context: PasswordPolicyContext)
}
