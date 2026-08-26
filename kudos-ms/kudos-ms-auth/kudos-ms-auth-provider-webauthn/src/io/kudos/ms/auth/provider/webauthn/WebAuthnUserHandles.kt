package io.kudos.ms.auth.provider.webauthn

import com.yubico.webauthn.data.ByteArray as YubicoByteArray
import java.security.MessageDigest

/** Stable, opaque and tenant-bound WebAuthn user handles. */
internal object WebAuthnUserHandles {
    fun derive(tenantId: String, userId: String): YubicoByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$tenantId\u0000$userId".toByteArray(Charsets.UTF_8))
        return YubicoByteArray(digest)
    }
}
