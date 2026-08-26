package io.kudos.ms.auth.provider.webauthn.protocol.attestation

import com.yubico.fido.metadata.AAGUID
import com.yubico.fido.metadata.FidoMetadataDownloader
import com.yubico.fido.metadata.FidoMetadataService
import com.yubico.fido.metadata.StatusReport
import com.yubico.webauthn.attestation.AttestationTrustSource
import com.yubico.webauthn.data.ByteArray as YubicoByteArray
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

internal fun interface IFidoAuthenticatorStatusLookup {
    /** Null means that this AAGUID has no entry in the verified metadata BLOB. */
    fun findStatusCodes(aaguid: String): Set<String>?
}

internal data class FidoMdsSnapshot(
    val trustSource: AttestationTrustSource,
    val statusLookup: IFidoAuthenticatorStatusLookup,
    val blobNumber: Int,
    val nextUpdate: LocalDate,
)

internal fun interface IFidoMetadataLoader {
    fun load(): FidoMdsSnapshot
}

internal class YubicoFidoMetadataLoader(
    configuration: ValidatedFidoMdsConfiguration,
    private val clock: Clock = Clock.systemUTC(),
) : IFidoMetadataLoader {
    private val downloader: FidoMetadataDownloader

    init {
        val directory = configuration.cacheDirectory
        try {
            Files.createDirectories(directory)
            if (!Files.isDirectory(directory) || !Files.isWritable(directory)) {
                fail("WEBAUTHN_FIDO_MDS_CACHE_DIRECTORY_UNAVAILABLE")
            }
        } catch (e: WebAuthnProviderException) {
            throw e
        } catch (e: Exception) {
            fail("WEBAUTHN_FIDO_MDS_CACHE_DIRECTORY_UNAVAILABLE", e)
        }
        downloader = FidoMetadataDownloader.builder()
            .expectLegalHeader(*configuration.legalHeaders.toTypedArray())
            .useDefaultTrustRoot()
            .useTrustRootCacheFile(directory.resolve(TRUST_ROOT_CACHE_FILE).toFile())
            .useDefaultBlob()
            .useBlobCacheFile(directory.resolve(BLOB_CACHE_FILE).toFile())
            .clock(clock)
            .verifyDownloadsOnly(true)
            .build()
    }

    @Synchronized
    override fun load(): FidoMdsSnapshot {
        val blob = downloader.loadCachedBlob()
        val unfilteredMetadata = FidoMetadataService.builder()
            .useBlob(blob)
            .prefilter { true }
            .filter { true }
            .build()
        return FidoMdsSnapshot(
            trustSource = FidoMetadataService.builder().useBlob(blob).build(),
            statusLookup = IFidoAuthenticatorStatusLookup { aaguid ->
                val today = LocalDate.now(clock)
                unfilteredMetadata.findEntries(aaguid.toYubicoAaguid())
                    .takeIf { it.isNotEmpty() }
                    ?.flatMap { it.statusReports }
                    ?.let { activeFidoStatusCodes(it, today) }
            },
            blobNumber = blob.payload.no,
            nextUpdate = blob.payload.nextUpdate,
        )
    }

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw WebAuthnProviderException(errorCode, cause)

    private fun String.toYubicoAaguid(): AAGUID {
        val uuid = UUID.fromString(this)
        val bytes = ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()
        return AAGUID(YubicoByteArray(bytes))
    }

    private companion object {
        const val TRUST_ROOT_CACHE_FILE = "fido-mds-trust-root.bin"
        const val BLOB_CACHE_FILE = "fido-mds-blob.jwt"
    }
}

internal fun activeFidoStatusCodes(reports: Iterable<StatusReport>, today: LocalDate): Set<String> = reports
    .filter { report -> report.effectiveDate.map { !it.isAfter(today) }.orElse(true) }
    .map { it.status.name }
    .toSortedSet()
