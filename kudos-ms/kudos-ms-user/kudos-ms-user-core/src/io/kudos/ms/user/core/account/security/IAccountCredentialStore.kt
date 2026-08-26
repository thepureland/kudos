package io.kudos.ms.user.core.account.security

/**
 * Optional in-process bridge to the credential owner, `kudos-ms-auth`.
 *
 * The login password's home is `auth_credential`; `user_account.login_password` remains only so that
 * `kudos-ms-user-core` stays independently deployable, exactly as [IPasswordHistory] does. When an
 * implementation is present it is the source of truth and the column is neither read nor written; when it is
 * absent the column is used as before.
 *
 * **Why verification is a boolean and not a hash getter.** Checking a password means running the encoder over
 * the plaintext *and* the stored hash together, so somebody has to hold both. Handing the hash back to this
 * module to do the comparison would put the stored credential into a second process boundary for no gain —
 * the design is explicit that a password hash is never returned across an API. So the plaintext goes in, a
 * verdict comes out, and the hash stays where it lives.
 *
 * Raw passwords passed here are used only for that comparison and must never be persisted or logged.
 * Implementations should fail closed when their backing store is unavailable.
 */
interface IAccountCredentialStore {

    /** Whether the account currently has a login password in the credential store. */
    fun hasPassword(context: PasswordPolicyContext): Boolean

    /** Whether [plainPassword] verifies against the stored credential. */
    fun verifyPassword(plainPassword: String, context: PasswordPolicyContext): Boolean

    /**
     * Enrols or rotates the login password.
     *
     * [encodedPassword] is already encoded by the caller's policy, so the encoder and its upgrade rules stay
     * in one place rather than being duplicated on both sides of this boundary.
     */
    fun storePassword(encodedPassword: String, context: PasswordPolicyContext)

    /**
     * Re-encodes the stored password after a successful sign-in when its encoding is out of date.
     *
     * Both halves — deciding that an upgrade is due, and replacing the value without clobbering a concurrent
     * password change — happen on the side that holds the hash. An earlier shape passed the expected hash in
     * so the caller could do the compare-and-set, which would have meant handing the stored credential back
     * across this boundary for no reason other than where the code happened to sit.
     *
     * @return true when this call performed an upgrade
     */
    fun upgradePasswordEncodingIfNeeded(plainPassword: String, context: PasswordPolicyContext): Boolean
}
