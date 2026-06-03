package io.kudos.ability.security.common.support

/**
 * One-time-password authenticator abstraction.
 *
 * The interface is intentionally minimal: three primitives that any OTP scheme exposes. Issue a
 * shared secret, render the current code, verify a user-supplied code. Apps that need extras
 * (HOTP counter, push, WebAuthn) should add a separate interface rather than overloading this one.
 *
 * Ported from soul's `org.soul.ability.security.common.otp.Authenticator` with two cleanups:
 *  - Drop `vendor/` sub-package. Soul split impls by vendor (Google / Microsoft), but the Google
 *    Authenticator and Microsoft Authenticator apps are both compatible with the same RFC 6238
 *    TOTP scheme. Multiple vendor classes were duplicating one algorithm.
 *  - Drop the `AuthenticatorFactory` + `AuthenticatorType` enum. With a single impl, the factory
 *    is pure boilerplate; with multiple impls, Spring's bean-name-qualified injection handles it
 *    natively.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface Authenticator {

    /**
     * Generate a fresh shared secret. Persist this server-side, associated with the user, and
     * also present it to the user (typically via QR code) to register in their authenticator app.
     */
    fun generateKey(): String

    /**
     * Verify a user-supplied code against the stored secret for the current time window.
     *
     * @return true if the code matches the current window (within the implementation's tolerance).
     */
    fun verify(secret: String, code: Int): Boolean

    /** Compute the code that the user's authenticator app should be showing right now. */
    fun generateCode(secret: String): String
}
