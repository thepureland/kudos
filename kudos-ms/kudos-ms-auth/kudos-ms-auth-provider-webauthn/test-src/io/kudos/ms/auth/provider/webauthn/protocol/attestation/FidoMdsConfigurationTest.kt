package io.kudos.ms.auth.provider.webauthn.protocol.attestation

import io.kudos.ms.auth.provider.webauthn.FidoMdsProperties
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class FidoMdsConfigurationTest {

    @Test
    fun validatesAndNormalizesExplicitConfiguration(@TempDir tempDirectory: File) {
        val configuration = properties(
            cacheDirectory = File(tempDirectory, "metadata-cache").absolutePath,
            legalHeaders = setOf(" accepted header "),
            refreshIntervalSeconds = 300,
        ).validated()

        assertTrue(configuration.cacheDirectory.isAbsolute)
        assertEquals(setOf("accepted header"), configuration.legalHeaders)
        assertEquals(300, configuration.refreshInterval.seconds)
        YubicoFidoMetadataLoader(configuration)
        assertTrue(configuration.cacheDirectory.toFile().isDirectory)
    }

    @Test
    fun rejectsMissingCacheDirectoryLegalHeaderAndUnsafeRefreshInterval(@TempDir tempDirectory: File) {
        assertError("WEBAUTHN_FIDO_MDS_CACHE_DIRECTORY_INVALID", properties(cacheDirectory = ""))
        assertError(
            "WEBAUTHN_FIDO_MDS_LEGAL_HEADER_INVALID",
            properties(cacheDirectory = tempDirectory.absolutePath, legalHeaders = emptySet()),
        )
        assertError(
            "WEBAUTHN_FIDO_MDS_REFRESH_INTERVAL_INVALID",
            properties(cacheDirectory = tempDirectory.absolutePath, refreshIntervalSeconds = 59),
        )
    }

    private fun assertError(expected: String, properties: FidoMdsProperties) {
        val error = assertFailsWith<WebAuthnProviderException> { properties.validated() }
        assertEquals(expected, error.errorCode)
    }

    private fun properties(
        cacheDirectory: String,
        legalHeaders: Set<String> = setOf("accepted header"),
        refreshIntervalSeconds: Long = 3_600,
    ) = FidoMdsProperties().apply {
        this.cacheDirectory = cacheDirectory
        this.legalHeaders = legalHeaders
        this.refreshIntervalSeconds = refreshIntervalSeconds
    }
}
