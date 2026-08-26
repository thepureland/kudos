package io.kudos.ms.auth.provider.oauth2.service

import io.kudos.ms.auth.common.provider.enums.ExternalJitUsernameStrategyEnum
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/** Produces a stable tenant-local username without treating a mutable claim as an account key. */
internal object ExternalJitUsernameGenerator {

    fun generate(
        resolved: ResolvedExternalPrincipal,
        strategy: ExternalJitUsernameStrategyEnum = ExternalJitUsernameStrategyEnum.EXTERNAL_USERNAME_HASHED,
    ): String {
        val principal = resolved.principal
        val stableHash = sha256(
            listOf(resolved.tenantId, resolved.providerId, principal.issuer.orEmpty(), principal.subject)
                .joinToString(separator = "|") { "${it.length}:$it" }
        )
        if (strategy == ExternalJitUsernameStrategyEnum.OPAQUE_HASHED) return "ext_${stableHash.take(28)}"
        val source = when (strategy) {
            ExternalJitUsernameStrategyEnum.EXTERNAL_USERNAME_HASHED -> principal.username ?: resolved.providerCode
            ExternalJitUsernameStrategyEnum.EMAIL_LOCAL_PART_HASHED ->
                principal.email?.substringBeforeLast('@') ?: resolved.providerCode
            ExternalJitUsernameStrategyEnum.OPAQUE_HASHED -> error("handled above")
        }
        val friendlyBase = sanitize(source, FRIENDLY_BASE_LENGTH)
        return if (friendlyBase.isBlank()) {
            "ext_${stableHash.take(28)}"
        } else {
            "${friendlyBase}_${stableHash.take(16)}"
        }
    }

    private fun sanitize(value: String, maxCodePoints: Int): String {
        val normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        val result = StringBuilder()
        val codePoints = normalized.codePoints().iterator()
        var pendingSeparator = false
        var appended = 0
        while (codePoints.hasNext() && appended < maxCodePoints) {
            val codePoint = codePoints.nextInt()
            if (Character.isLetterOrDigit(codePoint)) {
                if (pendingSeparator && result.isNotEmpty() && appended < maxCodePoints) {
                    result.append('_')
                    appended++
                }
                if (appended < maxCodePoints) {
                    result.appendCodePoint(codePoint)
                    appended++
                }
                pendingSeparator = false
            } else if (result.isNotEmpty()) {
                pendingSeparator = true
            }
        }
        return result.toString()
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private const val FRIENDLY_BASE_LENGTH = 15
}
