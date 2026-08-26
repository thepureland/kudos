package io.kudos.ms.auth.provider.webauthn.protocol.attestation

import com.yubico.webauthn.attestation.AttestationTrustSource
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import java.time.Duration
import java.time.LocalDate
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ReloadingFidoMdsAttestationTrustSourceTest {

    @Test
    fun initialLoadPublishesVerifiedSnapshotAndStartsRefresh() {
        val trustSource = trustSource()
        val scheduler = ManualScheduler()
        val source = ReloadingFidoMdsAttestationTrustSource(
            loader = IFidoMetadataLoader { snapshot(trustSource, 7) },
            refreshInterval = Duration.ofMinutes(15),
            scheduler = scheduler,
        )

        source.afterPropertiesSet()

        assertEquals(Duration.ofMinutes(15), scheduler.interval)
        assertEquals(
            trustSource.findTrustRoots(emptyList(), Optional.empty()),
            source.findTrustRoots(emptyList(), Optional.empty()),
        )
        source.destroy()
        assertTrue(scheduler.closed)
    }

    @Test
    fun successfulRefreshAtomicallyReplacesSnapshot() {
        val first = trustSource(enableRevocationChecking = false)
        val second = trustSource(enableRevocationChecking = true)
        val snapshots = ArrayDeque(
            listOf(
                snapshot(first, 7, setOf("UPDATE_AVAILABLE")),
                snapshot(second, 8, setOf("REVOKED")),
            )
        )
        val scheduler = ManualScheduler()
        val source = ReloadingFidoMdsAttestationTrustSource(
            loader = IFidoMetadataLoader { snapshots.removeFirst() },
            refreshInterval = Duration.ofHours(1),
            scheduler = scheduler,
        )
        source.afterPropertiesSet()
        assertEquals(
            WebAuthnAuthenticatorRiskLevelEnum.WARNING,
            source.evaluate("00000000-0000-0000-0000-000000000001").level,
        )

        scheduler.trigger()

        assertTrue(source.findTrustRoots(emptyList(), Optional.empty()).isEnableRevocationChecking)
        assertEquals(
            WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
            source.evaluate("00000000-0000-0000-0000-000000000001").level,
        )
    }

    @Test
    fun failedRefreshRetainsLastVerifiedSnapshot() {
        val first = trustSource(enableRevocationChecking = true)
        var firstLoad = true
        val scheduler = ManualScheduler()
        val source = ReloadingFidoMdsAttestationTrustSource(
            loader = IFidoMetadataLoader {
                if (firstLoad) {
                    firstLoad = false
                    snapshot(first, 7)
                } else {
                    throw IllegalStateException("network unavailable")
                }
            },
            refreshInterval = Duration.ofHours(1),
            scheduler = scheduler,
        )
        source.afterPropertiesSet()

        scheduler.trigger()

        assertTrue(source.findTrustRoots(emptyList(), Optional.empty()).isEnableRevocationChecking)
    }

    @Test
    fun initialLoadFailureClosesSchedulerAndFailsStartup() {
        val scheduler = ManualScheduler()
        val source = ReloadingFidoMdsAttestationTrustSource(
            loader = IFidoMetadataLoader { throw IllegalStateException("no valid cache") },
            refreshInterval = Duration.ofHours(1),
            scheduler = scheduler,
        )

        val error = assertFailsWith<WebAuthnProviderException> { source.afterPropertiesSet() }

        assertEquals("WEBAUTHN_FIDO_MDS_INITIAL_LOAD_FAILED", error.errorCode)
        assertTrue(scheduler.closed)
        assertFalse(scheduler.started)
    }

    @Test
    fun schedulerFailureClosesResourcesAndFailsStartup() {
        val scheduler = ManualScheduler(failOnStart = true)
        val source = ReloadingFidoMdsAttestationTrustSource(
            loader = IFidoMetadataLoader { snapshot(trustSource(), 7) },
            refreshInterval = Duration.ofHours(1),
            scheduler = scheduler,
        )

        val error = assertFailsWith<WebAuthnProviderException> { source.afterPropertiesSet() }

        assertEquals("WEBAUTHN_FIDO_MDS_REFRESH_SCHEDULER_FAILED", error.errorCode)
        assertTrue(scheduler.closed)
    }

    @Test
    fun riskAssessmentUsesStrongestStatusFromUnfilteredMetadata() {
        val source = ReloadingFidoMdsAttestationTrustSource(
            loader = IFidoMetadataLoader {
                snapshot(
                    trustSource(),
                    7,
                    setOf("FIDO_CERTIFIED_L2", "UPDATE_AVAILABLE", "REVOKED"),
                )
            },
            refreshInterval = Duration.ofHours(1),
            scheduler = ManualScheduler(),
        )
        source.afterPropertiesSet()

        val assessment = source.evaluate("00000000-0000-0000-0000-000000000001")

        assertEquals(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL, assessment.level)
        assertEquals(setOf("FIDO_CERTIFIED_L2", "REVOKED", "UPDATE_AVAILABLE"), assessment.statusCodes)
    }

    @Test
    fun missingMetadataIsExplicitlyNotEvaluated() {
        val source = ReloadingFidoMdsAttestationTrustSource(
            loader = IFidoMetadataLoader { snapshot(trustSource(), 7, null) },
            refreshInterval = Duration.ofHours(1),
            scheduler = ManualScheduler(),
        )
        source.afterPropertiesSet()

        val assessment = source.evaluate("00000000-0000-0000-0000-000000000001")

        assertEquals(WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED, assessment.level)
        assertEquals(setOf("METADATA_NOT_FOUND"), assessment.statusCodes)
    }

    private fun snapshot(
        source: AttestationTrustSource,
        number: Int,
        statusCodes: Set<String>? = emptySet(),
    ) = FidoMdsSnapshot(
        trustSource = source,
        statusLookup = IFidoAuthenticatorStatusLookup { statusCodes },
        blobNumber = number,
        nextUpdate = LocalDate.of(2026, 9, number),
    )

    private fun trustSource(enableRevocationChecking: Boolean = false): AttestationTrustSource {
        val roots = AttestationTrustSource.TrustRootsResult.builder()
            .trustRoots(emptySet())
            .enableRevocationChecking(enableRevocationChecking)
            .build()
        return AttestationTrustSource { _, _ -> roots }
    }

    private class ManualScheduler(
        private val failOnStart: Boolean = false,
    ) : IFidoMdsRefreshScheduler {
        var interval: Duration? = null
        var started: Boolean = false
        var closed: Boolean = false
        private var task: (() -> Unit)? = null

        override fun start(task: () -> Unit, interval: Duration) {
            if (failOnStart) throw IllegalStateException("scheduler unavailable")
            this.task = task
            this.interval = interval
            started = true
        }

        fun trigger() = requireNotNull(task).invoke()

        override fun close() {
            closed = true
        }
    }
}
