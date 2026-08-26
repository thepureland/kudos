package io.kudos.ms.auth.provider.webauthn.protocol.attestation

import com.yubico.fido.metadata.AuthenticatorStatus
import com.yubico.fido.metadata.StatusReport
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FidoMetadataStatusTest {

    @Test
    fun includesUndatedAndEffectiveStatusesButExcludesFutureStatuses() {
        val today = LocalDate.of(2026, 8, 25)
        val reports = listOf(
            report(AuthenticatorStatus.FIDO_CERTIFIED_L2),
            report(AuthenticatorStatus.UPDATE_AVAILABLE, today),
            report(AuthenticatorStatus.REVOKED, today.plusDays(1)),
        )

        assertEquals(
            setOf("FIDO_CERTIFIED_L2", "UPDATE_AVAILABLE"),
            activeFidoStatusCodes(reports, today),
        )
    }

    private fun report(status: AuthenticatorStatus, effectiveDate: LocalDate? = null): StatusReport {
        val builder = StatusReport.builder().status(status)
        effectiveDate?.let(builder::effectiveDate)
        return builder.build()
    }
}
