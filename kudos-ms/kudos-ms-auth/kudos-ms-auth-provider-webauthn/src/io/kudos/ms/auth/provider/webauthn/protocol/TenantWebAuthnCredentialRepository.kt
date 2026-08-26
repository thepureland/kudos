package io.kudos.ms.auth.provider.webauthn.protocol

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.AuthenticatorTransport
import com.yubico.webauthn.data.ByteArray as YubicoByteArray
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import java.util.Optional

/** Tenant-scoped bridge from Kudos public-key storage to Yubico's read-only credential contract. */
@Suppress("DEPRECATION")
internal class TenantWebAuthnCredentialRepository(
    private val tenantId: String,
    private val credentialService: IWebAuthnCredentialService,
    private val userAccountService: IUserAccountService,
    private val knownUsername: String? = null,
    private val knownUserHandle: YubicoByteArray? = null,
    knownCredentialIds: Set<String> = emptySet(),
) : CredentialRepository {
    private val descriptors = knownCredentialIds.mapTo(linkedSetOf()) { credentialId ->
        PublicKeyCredentialDescriptor.builder().id(YubicoByteArray.fromBase64Url(credentialId)).build()
    }

    override fun getCredentialIdsForUsername(username: String): Set<PublicKeyCredentialDescriptor> =
        descriptors.takeIf { username == knownUsername } ?: emptySet()

    override fun getUserHandleForUsername(username: String): Optional<YubicoByteArray> =
        Optional.ofNullable(knownUserHandle?.takeIf { username == knownUsername })

    override fun getUsernameForUserHandle(userHandle: YubicoByteArray): Optional<String> {
        if (knownUserHandle == userHandle && knownUsername != null) return Optional.of(knownUsername)
        val userId = credentialService.findActiveUserIdByUserHandle(tenantId, userHandle.base64Url)
            ?: return Optional.empty()
        val account = userAccountService.get(userId)
            ?.takeIf { it.tenantId == tenantId && it.active }
            ?: return Optional.empty()
        return Optional.of(account.username)
    }

    override fun lookup(
        credentialId: YubicoByteArray,
        userHandle: YubicoByteArray,
    ): Optional<RegisteredCredential> = Optional.ofNullable(
        credentialService.findActive(tenantId, credentialId.base64Url)
            ?.takeIf { YubicoByteArray.fromBase64Url(it.userHandle) == userHandle }
            ?.toRegisteredCredential()
    )

    override fun lookupAll(credentialId: YubicoByteArray): Set<RegisteredCredential> =
        credentialService.findActive(tenantId, credentialId.base64Url)
            ?.toRegisteredCredential()
            ?.let(::setOf)
            ?: emptySet()

    private fun AuthWebAuthnCredential.toRegisteredCredential(): RegisteredCredential =
        RegisteredCredential.builder()
            .credentialId(YubicoByteArray.fromBase64Url(credentialId))
            .userHandle(YubicoByteArray.fromBase64Url(userHandle))
            .publicKeyCose(YubicoByteArray.fromBase64Url(publicKeyCose))
            .signatureCount(signatureCount)
            .transports(transports.toAuthenticatorTransports())
            .backupEligible(backupEligible)
            .backupState(backedUp)
            .build()

    private fun String?.toAuthenticatorTransports(): Set<AuthenticatorTransport> =
        this?.split(',')
            ?.filter { it.isNotBlank() }
            ?.mapTo(linkedSetOf(), AuthenticatorTransport::of)
            ?: emptySet()
}
