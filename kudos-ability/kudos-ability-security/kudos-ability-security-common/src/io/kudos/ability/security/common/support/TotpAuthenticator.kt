package io.kudos.ability.security.common.support

import io.kudos.base.security.GoogleAuthenticator
import org.apache.commons.codec.binary.Base32
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.Clock
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 TOTP implementation, compatible with Google Authenticator, Microsoft Authenticator,
 * Authy, 1Password, and every other standard TOTP app.
 *
 * Why one class and not "GoogleAuthenticator / MicrosoftAuthenticator": soul split implementations
 * per vendor, but the apps speak the same protocol (a single 30-second SHA1 HMAC TOTP). The split
 * was cosmetic; this class serves both.
 *
 * Improvements over soul's `GoogleAuthenticator`:
 *  1. Non-deterministic key generation. Soul (and the inherited `kudos-base` impl) seeds
 *     `SecureRandom` with a fixed Base64 string before generating the secret. That mixes the seed
 *     into the entropy pool rather than replacing it, but is concerning if anyone leans on it as
 *     the sole randomness source. This class uses a fresh `SecureRandom` with no seeding,
 *     leaving entropy to the JDK default.
 *  2. Stricter default window. Soul's `windowSize=3` (±90 sec drift) is too permissive for a
 *     30-sec TOTP, widening the brute-force search by 6x. Default here is 1 (±30 sec); business
 *     apps with known device clock skew can override.
 *  3. Throws on crypto failure instead of `printStackTrace + return null` / `return false`. A
 *     failed HMAC-SHA1 is a JVM misconfiguration, not a verification result.
 *  4. Injectable [Clock] for testability. Soul calls `System.currentTimeMillis()` directly.
 *
 * Verification delegates to [GoogleAuthenticator.checkCode] in kudos-base since that already has
 * the battle-tested sign-extension fix. Code generation is reimplemented here because the
 * underlying `verifyCode` helper in kudos-base is `internal` and not exposed cross-module; the
 * algorithm is short and the duplication is one HMAC-SHA1 + truncation block.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class TotpAuthenticator(
    windowSize: Int = 1,
    private val clock: Clock = Clock.systemUTC(),
    private val secureRandom: SecureRandom = SecureRandom(),
) : Authenticator {

    private val delegate = GoogleAuthenticator().apply { this.windowSize = windowSize }

    override fun generateKey(): String {
        val buffer = ByteArray(GoogleAuthenticator.SECRET_SIZE)
        secureRandom.nextBytes(buffer)
        return String(Base32().encode(buffer))
    }

    override fun verify(secret: String, code: Int): Boolean =
        delegate.checkCode(secret, code.toLong(), clock.millis())

    override fun generateCode(secret: String): String {
        val key = Base32().decode(secret)
        val timeWindow = clock.millis() / 1000L / TIME_STEP_SECONDS
        val code = computeTotp(key, timeWindow)
        return "%06d".format(code)
    }

    /**
     * RFC 6238 HMAC-SHA1 + RFC 4226 dynamic truncation.
     *
     * Sign-extension note: assembling the 31-bit truncated hash requires reading each hash byte
     * as unsigned. `hash[i].toInt()` sign-extends bytes >= 0x80, so we mask with `and 0xFF`
     * before shifting. Without the mask, codes don't match standard TOTP apps for ~half of all
     * inputs. (Same bug fix as the comment on `GoogleAuthenticator.verifyCode` in kudos-base.)
     */
    private fun computeTotp(key: ByteArray, timeWindow: Long): Int {
        val data = ByteBuffer.allocate(8).putLong(timeWindow).array()
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(key, "HmacSHA1")) }
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 0xF
        val binary =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        return binary % 1_000_000
    }

    private companion object {
        const val TIME_STEP_SECONDS = 30L
    }
}
