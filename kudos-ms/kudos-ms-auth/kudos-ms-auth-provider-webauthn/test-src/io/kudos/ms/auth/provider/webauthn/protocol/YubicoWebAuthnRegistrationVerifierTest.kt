package io.kudos.ms.auth.provider.webauthn.protocol

import com.upokecenter.cbor.CBORObject
import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria
import com.yubico.webauthn.data.ByteArray as YubicoByteArray
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import com.yubico.webauthn.data.RegistrationExtensionInputs
import com.yubico.webauthn.data.RelyingPartyIdentity
import com.yubico.webauthn.data.UserIdentity
import com.yubico.webauthn.data.UserVerificationRequirement
import com.yubico.webauthn.exception.RegistrationFailedException
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Exercises a complete, cryptographically valid `none` attestation without a browser or mocked Yubico result. */
@Suppress("DEPRECATION")
internal class YubicoWebAuthnRegistrationVerifierTest {
    private val rpId = "example.com"
    private val origin = "https://login.example.com"
    private val repository = EmptyCredentialRepository()

    @Test
    fun `should verify complete none attestation and return public credential material`() {
        val request = registrationRequest()
        val fixture = registrationResponse(request)

        val result = YubicoWebAuthnRegistrationVerifier().verify(
            WebAuthnRegistrationVerificationRequest(
                request = request,
                credentialResponseJson = fixture.responseJson,
                rpId = rpId,
                rpName = "Kudos Test",
                origins = setOf(origin),
                credentialRepository = repository,
                clock = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC),
            )
        )

        assertEquals(fixture.credentialId.base64Url, result.credentialId)
        assertEquals(fixture.publicKeyCose.base64Url, result.publicKeyCose)
        assertEquals(setOf("internal"), result.transports)
        assertEquals("00000000-0000-0000-0000-000000000000", result.aaguid)
        assertEquals("none", result.attestationFormat)
        assertTrue(result.userVerified)
        assertTrue(result.discoverable)
        assertFalse(result.attestationTrusted)
        assertFalse(result.backupEligible)
        assertFalse(result.backedUp)
    }

    @Test
    fun `should reject response with a different challenge`() {
        val request = registrationRequest()
        val fixture = registrationResponse(
            request,
            challenge = YubicoByteArray(ByteArray(32) { 7 }).base64Url,
        )

        assertFailsWith<RegistrationFailedException> {
            verify(request, fixture.responseJson)
        }
    }

    @Test
    fun `should reject response from an untrusted origin`() {
        val request = registrationRequest()
        val fixture = registrationResponse(request, clientOrigin = "https://attacker.example.net")

        assertFailsWith<RegistrationFailedException> {
            verify(request, fixture.responseJson)
        }
    }

    private fun verify(
        request: PublicKeyCredentialCreationOptions,
        responseJson: String,
    ): VerifiedWebAuthnRegistrationResult = YubicoWebAuthnRegistrationVerifier().verify(
        WebAuthnRegistrationVerificationRequest(
            request = request,
            credentialResponseJson = responseJson,
            rpId = rpId,
            rpName = "Kudos Test",
            origins = setOf(origin),
            credentialRepository = repository,
            clock = Clock.fixed(Instant.parse("2026-08-25T10:00:00Z"), ZoneOffset.UTC),
        )
    )

    private fun registrationRequest(): PublicKeyCredentialCreationOptions = RelyingParty.builder()
        .identity(RelyingPartyIdentity.builder().id(rpId).name("Kudos Test").build())
        .credentialRepository(repository)
        .origins(setOf(origin))
        .build()
        .startRegistration(
            StartRegistrationOptions.builder()
                .user(
                    UserIdentity.builder()
                        .name("alice")
                        .displayName("alice")
                        .id(YubicoByteArray(MessageDigest.getInstance("SHA-256").digest("alice".toByteArray())))
                        .build()
                )
                .authenticatorSelection(
                    AuthenticatorSelectionCriteria.builder()
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build()
                )
                .extensions(RegistrationExtensionInputs.builder().credProps().build())
                .build()
        )

    private fun registrationResponse(
        request: PublicKeyCredentialCreationOptions,
        challenge: String = request.challenge.base64Url,
        clientOrigin: String = origin,
    ): RegistrationFixture {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val publicKey = keyPair.public as ECPublicKey
        val coseKey = CBORObject.NewMap()
            .Add(1, 2)
            .Add(3, -7)
            .Add(-1, 1)
            .Add(-2, publicKey.w.affineX.fixedUnsigned(32))
            .Add(-3, publicKey.w.affineY.fixedUnsigned(32))
            .EncodeToBytes()
        val credentialId = MessageDigest.getInstance("SHA-256").digest("credential-new".toByteArray())
        val rpIdHash = MessageDigest.getInstance("SHA-256").digest(rpId.toByteArray())
        val authenticatorData = ByteBuffer.allocate(32 + 1 + 4 + 16 + 2 + credentialId.size + coseKey.size)
            .put(rpIdHash)
            .put(0x45.toByte()) // UP + UV + AT
            .putInt(0)
            .put(ByteArray(16))
            .putShort(credentialId.size.toShort())
            .put(credentialId)
            .put(coseKey)
            .array()
        val attestationObject = CBORObject.NewMap()
            .Add("fmt", "none")
            .Add("attStmt", CBORObject.NewMap())
            .Add("authData", authenticatorData)
            .EncodeToBytes()
        val clientDataJson = """{"type":"webauthn.create","challenge":"$challenge","origin":"$clientOrigin","crossOrigin":false}"""
        val id = YubicoByteArray(credentialId).base64Url
        val responseJson = """
            {
              "id":"$id",
              "rawId":"$id",
              "type":"public-key",
              "authenticatorAttachment":"platform",
              "response":{
                "attestationObject":"${YubicoByteArray(attestationObject).base64Url}",
                "clientDataJSON":"${YubicoByteArray(clientDataJson.toByteArray()).base64Url}",
                "transports":["internal"]
              },
              "clientExtensionResults":{"credProps":{"rk":true}}
            }
        """.trimIndent()
        return RegistrationFixture(
            responseJson = responseJson,
            credentialId = YubicoByteArray(credentialId),
            publicKeyCose = YubicoByteArray(coseKey),
        )
    }

    private fun BigInteger.fixedUnsigned(size: Int): ByteArray {
        val source = toByteArray().let { if (it.size == size + 1 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        require(source.size <= size)
        return ByteArray(size).also { source.copyInto(it, size - source.size) }
    }

    private data class RegistrationFixture(
        val responseJson: String,
        val credentialId: YubicoByteArray,
        val publicKeyCose: YubicoByteArray,
    )

    private class EmptyCredentialRepository : CredentialRepository {
        override fun getCredentialIdsForUsername(username: String): Set<PublicKeyCredentialDescriptor> = emptySet()
        override fun getUserHandleForUsername(username: String): Optional<YubicoByteArray> = Optional.empty()
        override fun getUsernameForUserHandle(userHandle: YubicoByteArray): Optional<String> = Optional.empty()
        override fun lookup(
            credentialId: YubicoByteArray,
            userHandle: YubicoByteArray,
        ): Optional<RegisteredCredential> = Optional.empty()

        override fun lookupAll(credentialId: YubicoByteArray): Set<RegisteredCredential> = emptySet()
    }
}
