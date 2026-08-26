package io.kudos.ms.auth.provider.webauthn.protocol.attestation

import com.yubico.webauthn.attestation.AttestationTrustSource
import com.yubico.webauthn.data.ByteArray as YubicoByteArray
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.IWebAuthnAuthenticatorRiskEvaluator
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskAssessment
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.provider.webauthn.model.WebAuthnProviderException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.Optional
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal interface IFidoMdsRefreshScheduler : AutoCloseable {
    fun start(task: () -> Unit, interval: Duration)
}

internal class DaemonFidoMdsRefreshScheduler : IFidoMdsRefreshScheduler {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "kudos-webauthn-fido-mds-refresh").apply { isDaemon = true }
    }

    override fun start(task: () -> Unit, interval: Duration) {
        executor.scheduleWithFixedDelay(task, interval.seconds, interval.seconds, TimeUnit.SECONDS)
    }

    override fun close() {
        executor.shutdownNow()
    }
}

/** Atomically replaces immutable FIDO MDS snapshots after each verified refresh. */
class ReloadingFidoMdsAttestationTrustSource internal constructor(
    private val loader: IFidoMetadataLoader,
    private val refreshInterval: Duration,
    private val scheduler: IFidoMdsRefreshScheduler = DaemonFidoMdsRefreshScheduler(),
) : AttestationTrustSource, IWebAuthnAuthenticatorRiskEvaluator, InitializingBean, DisposableBean {
    private val snapshot = AtomicReference<FidoMdsSnapshot>()
    private val started = AtomicBoolean(false)

    override fun afterPropertiesSet() {
        if (!started.compareAndSet(false, true)) return
        val initial = try {
            loader.load()
        } catch (e: Exception) {
            scheduler.close()
            throw WebAuthnProviderException("WEBAUTHN_FIDO_MDS_INITIAL_LOAD_FAILED", e)
        }
        snapshot.set(initial)
        log.info("Loaded FIDO MDS BLOB no={} nextUpdate={}", initial.blobNumber, initial.nextUpdate)
        try {
            scheduler.start(::refresh, refreshInterval)
        } catch (e: Exception) {
            scheduler.close()
            throw WebAuthnProviderException("WEBAUTHN_FIDO_MDS_REFRESH_SCHEDULER_FAILED", e)
        }
    }

    override fun findTrustRoots(
        attestationCertificateChain: List<X509Certificate>,
        aaguid: Optional<YubicoByteArray>,
    ): AttestationTrustSource.TrustRootsResult = current().trustSource.findTrustRoots(
        attestationCertificateChain,
        aaguid,
    )

    override val source: String = "FIDO_MDS"

    override fun evaluate(aaguid: String): WebAuthnAuthenticatorRiskAssessment {
        val statusCodes = current().statusLookup.findStatusCodes(aaguid)
            ?: return WebAuthnAuthenticatorRiskAssessment(
                level = WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED,
                statusCodes = setOf("METADATA_NOT_FOUND"),
            )
        return WebAuthnAuthenticatorRiskAssessment(
            level = statusCodes.maxOfOrNull(::riskLevel)
                ?: WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED,
            statusCodes = statusCodes,
        )
    }

    override fun destroy() {
        scheduler.close()
    }

    internal fun refresh() {
        try {
            val updated = loader.load()
            snapshot.set(updated)
            log.info("Refreshed FIDO MDS BLOB no={} nextUpdate={}", updated.blobNumber, updated.nextUpdate)
        } catch (e: Exception) {
            val current = snapshot.get()
            log.error(
                "Failed to refresh FIDO MDS; retaining verified BLOB no={} nextUpdate={}",
                current?.blobNumber,
                current?.nextUpdate,
                e,
            )
        }
    }

    private fun current(): FidoMdsSnapshot = snapshot.get()
        ?: throw IllegalStateException("FIDO MDS trust source has not been initialized")

    private fun riskLevel(status: String): WebAuthnAuthenticatorRiskLevelEnum = when (status) {
        "ATTESTATION_KEY_COMPROMISE",
        "REVOKED",
        "USER_KEY_PHYSICAL_COMPROMISE",
        "USER_KEY_REMOTE_COMPROMISE",
        "USER_VERIFICATION_BYPASS",
        -> WebAuthnAuthenticatorRiskLevelEnum.CRITICAL

        "NOT_FIDO_CERTIFIED",
        "RETIRED",
        "SELF_ASSERTION_SUBMITTED",
        "UNKNOWN",
        "UPDATE_AVAILABLE",
        -> WebAuthnAuthenticatorRiskLevelEnum.WARNING

        "FIDO_CERTIFIED",
        "FIDO_CERTIFIED_L1",
        "FIDO_CERTIFIED_L1plus",
        "FIDO_CERTIFIED_L2",
        "FIDO_CERTIFIED_L2plus",
        "FIDO_CERTIFIED_L3",
        "FIDO_CERTIFIED_L3plus",
        -> WebAuthnAuthenticatorRiskLevelEnum.NORMAL

        else -> WebAuthnAuthenticatorRiskLevelEnum.WARNING
    }

    private companion object {
        val log = LoggerFactory.getLogger(ReloadingFidoMdsAttestationTrustSource::class.java)
    }
}
