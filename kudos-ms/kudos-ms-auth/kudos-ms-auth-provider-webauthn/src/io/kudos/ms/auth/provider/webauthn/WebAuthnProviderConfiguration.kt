package io.kudos.ms.auth.provider.webauthn

import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.data.UserVerificationRequirement
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import org.springframework.stereotype.Component
import java.net.URI

data class ValidatedWebAuthnProviderConfiguration(
    val rpId: String,
    val rpName: String,
    val origins: Set<String>,
    val ceremonyTtlSeconds: Long,
    val browserTimeoutMillis: Long,
    val userVerification: UserVerificationRequirement,
    val residentKey: ResidentKeyRequirement,
)

/** One fail-fast configuration boundary shared by registration and assertion ceremonies. */
@Component
open class WebAuthnProviderConfiguration(
    private val properties: WebAuthnProviderProperties,
) {
    open fun validated(): ValidatedWebAuthnProviderConfiguration {
        val rpId = properties.rpId.trim().lowercase()
        if (rpId.isBlank() || rpId.length > 253 || !RP_ID.matches(rpId)) fail("WEBAUTHN_RP_ID_INVALID")
        val rpName = properties.rpName.trim()
        if (rpName.isBlank() || rpName.length > 100) fail("WEBAUTHN_RP_NAME_INVALID")
        if (properties.ceremonyTtlSeconds !in 30..600) fail("WEBAUTHN_CEREMONY_TTL_INVALID")
        if (properties.browserTimeoutMillis !in 10_000..300_000) fail("WEBAUTHN_BROWSER_TIMEOUT_INVALID")
        val origins = properties.origins.map { validatedOrigin(it, rpId) }.toSet()
        if (origins.isEmpty()) fail("WEBAUTHN_ORIGIN_REQUIRED")
        return ValidatedWebAuthnProviderConfiguration(
            rpId = rpId,
            rpName = rpName,
            origins = origins,
            ceremonyTtlSeconds = properties.ceremonyTtlSeconds,
            browserTimeoutMillis = properties.browserTimeoutMillis,
            userVerification = enumValue(
                properties.userVerification,
                "WEBAUTHN_USER_VERIFICATION_INVALID",
            ),
            residentKey = enumValue(properties.residentKey, "WEBAUTHN_RESIDENT_KEY_INVALID"),
        )
    }

    private fun validatedOrigin(raw: String, rpId: String): String {
        val value = raw.trim()
        val uri = runCatching { URI(value) }.getOrElse { fail("WEBAUTHN_ORIGIN_INVALID", it) }
        val localhost = uri.host == "localhost" || uri.host == "127.0.0.1" || uri.host == "[::1]"
        val allowedScheme = uri.scheme == "https" ||
            properties.allowInsecureLocalhost && localhost && uri.scheme == "http"
        if (!allowedScheme || uri.host.isNullOrBlank() || uri.userInfo != null || uri.path !in setOf("", null) ||
            uri.query != null || uri.fragment != null
        ) {
            fail("WEBAUTHN_ORIGIN_INVALID")
        }
        val originHost = uri.host.lowercase().trimEnd('.')
        if (originHost != rpId && !originHost.endsWith(".$rpId")) fail("WEBAUTHN_ORIGIN_RP_MISMATCH")
        return value
    }

    private inline fun <reified E : Enum<E>> enumValue(raw: String, errorCode: String): E =
        runCatching { enumValueOf<E>(raw.trim().uppercase()) }.getOrElse { fail(errorCode, it) }

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw WebAuthnProviderException(errorCode, cause)

    private companion object {
        val RP_ID = Regex("^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)*[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
    }
}
