package io.kudos.ms.auth.provider.webauthn.protocol.attestation

import io.kudos.ms.auth.provider.webauthn.FidoMdsProperties
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.time.Duration

internal data class ValidatedFidoMdsConfiguration(
    val cacheDirectory: Path,
    val legalHeaders: Set<String>,
    val refreshInterval: Duration,
)

internal fun FidoMdsProperties.validated(): ValidatedFidoMdsConfiguration {
    val directory = try {
        Path.of(cacheDirectory.trim()).toAbsolutePath().normalize()
    } catch (e: InvalidPathException) {
        fail("WEBAUTHN_FIDO_MDS_CACHE_DIRECTORY_INVALID", e)
    }
    if (cacheDirectory.isBlank() || !Path.of(cacheDirectory.trim()).isAbsolute) {
        fail("WEBAUTHN_FIDO_MDS_CACHE_DIRECTORY_INVALID")
    }
    val headers = legalHeaders.map(String::trim).toSortedSet()
    if (headers.isEmpty() || headers.size > MAX_LEGAL_HEADERS || headers.any { it.isBlank() || it.length > 4_096 }) {
        fail("WEBAUTHN_FIDO_MDS_LEGAL_HEADER_INVALID")
    }
    if (refreshIntervalSeconds !in 60..86_400) {
        fail("WEBAUTHN_FIDO_MDS_REFRESH_INTERVAL_INVALID")
    }
    return ValidatedFidoMdsConfiguration(
        cacheDirectory = directory,
        legalHeaders = headers,
        refreshInterval = Duration.ofSeconds(refreshIntervalSeconds),
    )
}

private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
    throw WebAuthnProviderException(errorCode, cause)

private const val MAX_LEGAL_HEADERS = 8
