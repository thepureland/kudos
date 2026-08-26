package io.kudos.ms.auth.core.authentication.credentialrevocation.service.iservice

import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationCommand
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationResult

/** Administrator-initiated, audited removal of a credential the account can no longer be trusted to hold. */
interface IAuthCredentialRevocationService {

    /**
     * Revokes one credential and records who did it and why.
     *
     * Always carried out when the credential exists: a credential believed compromised has to stop working,
     * and refusing because it is the account's last factor would leave it usable. The result says what the
     * account was left with instead, so the operator can arrange a rescue rather than discover the lockout
     * from the user.
     */
    fun revoke(command: AuthCredentialRevocationCommand): AuthCredentialRevocationResult

    fun listRecent(tenantId: String, userId: String?, limit: Int): List<AuthCredentialRevocationResult>
}
