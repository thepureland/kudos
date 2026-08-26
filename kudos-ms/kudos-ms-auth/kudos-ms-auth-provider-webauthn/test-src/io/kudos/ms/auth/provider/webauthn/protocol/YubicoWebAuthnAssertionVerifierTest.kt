package io.kudos.ms.auth.provider.webauthn.protocol

import com.upokecenter.cbor.CBORObject
import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartAssertionOptions
import com.yubico.webauthn.data.ByteArray as YubicoByteArray
import com.yubico.webauthn.data.RelyingPartyIdentity
import com.yubico.webauthn.data.UserVerificationRequirement
import com.yubico.webauthn.exception.AssertionFailedException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Exercises complete username-less assertions signed by a dynamically generated authenticator key. */
@Suppress("DEPRECATION")
internal class YubicoWebAuthnAssertionVerifierTest {
    private val rpId = "example.com"
    private val origin = "https://login.example.com"
    private val userHandle = YubicoByteArray(MessageDigest.getInstance("SHA-256").digest("alice".toByteArray()))
    private val credentialId = YubicoByteArray(MessageDigest.getInstance("SHA-256").digest("credential-1".toByteArray()))
    private val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
    private val publicKeyCose = cosePublicKey(keyPair.public as ECPublicKey)
    private val credentialService = mock(IWebAuthnCredentialService::class.java)
    private val userAccountService = mock(IUserAccountService::class.java)
    private val repository by lazy {
        `when`(credentialService.findActive("tenant-1", credentialId.base64Url)).thenReturn(storedCredential())
        `when`(credentialService.findActiveUserIdByUserHandle("tenant-1", userHandle.base64Url))
            .thenReturn("user-1")
        `when`(userAccountService.get("user-1")).thenReturn(UserAccount {
            id = "user-1"
            username = "alice"
            tenantId = "tenant-1"
            active = true
        })
        TenantWebAuthnCredentialRepository(
            tenantId = "tenant-1",
            credentialService = credentialService,
            userAccountService = userAccountService,
        )
    }
    private val knownUserRepository by lazy {
        repository
        TenantWebAuthnCredentialRepository(
            tenantId = "tenant-1",
            credentialService = credentialService,
            userAccountService = userAccountService,
            knownUsername = "alice",
            knownUserHandle = userHandle,
            knownCredentialIds = setOf(credentialId.base64Url),
        )
    }

    @Test
    fun `should verify complete username-less assertion and return authenticated subject material`() {
        val request = assertionRequest()

        val result = verify(request, assertionResponse(request))

        assertEquals(credentialId.base64Url, result.credentialId)
        assertEquals(userHandle.base64Url, result.userHandle)
        assertEquals("alice", result.username)
        assertEquals(9, result.signatureCount)
        assertTrue(result.userVerified)
        assertTrue(result.backupEligible)
        assertTrue(result.backedUp)
    }

    @Test
    fun `should verify known-user assertion without browser supplied user handle`() {
        val request = assertionRequest(knownUserRepository, "alice")

        val result = verify(request, assertionResponse(request, includeUserHandle = false), knownUserRepository)

        assertEquals("alice", result.username)
        assertEquals(userHandle.base64Url, result.userHandle)
        assertEquals(credentialId.base64Url, result.credentialId)
    }

    @Test
    fun `should reject assertion signed for an untrusted origin`() {
        val request = assertionRequest()

        assertFailsWith<AssertionFailedException> {
            verify(request, assertionResponse(request, clientOrigin = "https://attacker.example.net"))
        }
    }

    @Test
    fun `should reject assertion with a different challenge`() {
        val request = assertionRequest()

        assertFailsWith<AssertionFailedException> {
            verify(
                request,
                assertionResponse(request, challenge = YubicoByteArray(ByteArray(32) { 7 }).base64Url),
            )
        }
    }

    private fun assertionRequest(
        credentialRepository: CredentialRepository = repository,
        username: String? = null,
    ) = RelyingParty.builder()
        .identity(RelyingPartyIdentity.builder().id(rpId).name("Kudos Test").build())
        .credentialRepository(credentialRepository)
        .origins(setOf(origin))
        .build()
        .startAssertion(
            StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.REQUIRED)
                .apply { username?.let(::username) }
                .build()
        )

    private fun verify(
        request: com.yubico.webauthn.AssertionRequest,
        responseJson: String,
        credentialRepository: CredentialRepository = repository,
    ): VerifiedWebAuthnAssertionResult = YubicoWebAuthnAssertionVerifier().verify(
        WebAuthnAssertionVerificationRequest(
            request = request,
            credentialResponseJson = responseJson,
            rpId = rpId,
            rpName = "Kudos Test",
            origins = setOf(origin),
            credentialRepository = credentialRepository,
            clock = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC),
        )
    )

    private fun assertionResponse(
        request: com.yubico.webauthn.AssertionRequest,
        challenge: String = request.publicKeyCredentialRequestOptions.challenge.base64Url,
        clientOrigin: String = origin,
        includeUserHandle: Boolean = true,
    ): String {
        val clientDataJson =
            """{"type":"webauthn.get","challenge":"$challenge","origin":"$clientOrigin","crossOrigin":false}"""
        val authenticatorData = ByteBuffer.allocate(37)
            .put(MessageDigest.getInstance("SHA-256").digest(rpId.toByteArray()))
            .put(0x1d.toByte()) // UP + UV + BE + BS
            .putInt(9)
            .array()
        val signedBytes = authenticatorData + MessageDigest.getInstance("SHA-256")
            .digest(clientDataJson.toByteArray())
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(signedBytes)
            sign()
        }
        val userHandleJson = if (includeUserHandle) ",\"userHandle\":\"${userHandle.base64Url}\"" else ""
        return """
            {
              "id":"${credentialId.base64Url}",
              "rawId":"${credentialId.base64Url}",
              "type":"public-key",
              "authenticatorAttachment":"platform",
              "response":{
                "authenticatorData":"${YubicoByteArray(authenticatorData).base64Url}",
                "clientDataJSON":"${YubicoByteArray(clientDataJson.toByteArray()).base64Url}",
                "signature":"${YubicoByteArray(signature).base64Url}"$userHandleJson
              },
              "clientExtensionResults":{}
            }
        """.trimIndent()
    }

    private fun cosePublicKey(publicKey: ECPublicKey): YubicoByteArray = YubicoByteArray(
        CBORObject.NewMap()
            .Add(1, 2)
            .Add(3, -7)
            .Add(-1, 1)
            .Add(-2, publicKey.w.affineX.fixedUnsigned(32))
            .Add(-3, publicKey.w.affineY.fixedUnsigned(32))
            .EncodeToBytes()
    )

    private fun BigInteger.fixedUnsigned(size: Int): ByteArray {
        val source = toByteArray().let {
            if (it.size == size + 1 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
        }
        require(source.size <= size)
        return ByteArray(size).also { source.copyInto(it, size - source.size) }
    }

    private fun storedCredential() = AuthWebAuthnCredential {
        id = "row-1"
        tenantId = "tenant-1"
        userId = "user-1"
        credentialId = this@YubicoWebAuthnAssertionVerifierTest.credentialId.base64Url
        userHandle = this@YubicoWebAuthnAssertionVerifierTest.userHandle.base64Url
        publicKeyCose = this@YubicoWebAuthnAssertionVerifierTest.publicKeyCose.base64Url
        signatureCount = 8
        transports = "internal"
        backupEligible = true
        backedUp = true
        discoverable = true
        displayName = "Alice passkey"
        createdAt = LocalDateTime.ofInstant(Instant.parse("2026-08-25T09:00:00Z"), ZoneOffset.UTC)
        version = 0
    }
}
