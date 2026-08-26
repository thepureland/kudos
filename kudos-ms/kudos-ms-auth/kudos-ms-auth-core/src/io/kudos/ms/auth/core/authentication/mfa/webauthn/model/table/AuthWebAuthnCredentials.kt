package io.kudos.ms.auth.core.authentication.mfa.webauthn.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import org.ktorm.schema.*

object AuthWebAuthnCredentials : StringIdTable<AuthWebAuthnCredential>("auth_webauthn_credential") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var credentialId = varchar("credential_id").bindTo { it.credentialId }
    var userHandle = varchar("user_handle").bindTo { it.userHandle }
    var publicKeyCose = varchar("public_key_cose").bindTo { it.publicKeyCose }
    var signatureCount = long("signature_count").bindTo { it.signatureCount }
    var transports = varchar("transports").bindTo { it.transports }
    var aaguid = varchar("aaguid").bindTo { it.aaguid }
    var attestationFormat = varchar("attestation_format").bindTo { it.attestationFormat }
    var backupEligible = boolean("backup_eligible").bindTo { it.backupEligible }
    var backedUp = boolean("backed_up").bindTo { it.backedUp }
    var discoverable = boolean("discoverable").bindTo { it.discoverable }
    var displayName = varchar("display_name").bindTo { it.displayName }
    var createdAt = datetime("created_at").bindTo { it.createdAt }
    var lastUsedAt = datetime("last_used_at").bindTo { it.lastUsedAt }
    var revokedAt = datetime("revoked_at").bindTo { it.revokedAt }
    var version = long("version").bindTo { it.version }
}
