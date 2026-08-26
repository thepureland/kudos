package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.po.AuthWebAuthnAttestationPolicy
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object AuthWebAuthnAttestationPolicies :
    StringIdTable<AuthWebAuthnAttestationPolicy>("auth_webauthn_attestation_policy") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var aaguidMode = varchar("aaguid_mode").bindTo { it.aaguidMode }
    var aaguids = text("aaguids").bindTo { it.aaguids }
    var allowedAttestationFormats = text("allowed_attestation_formats").bindTo { it.allowedAttestationFormats }
    var requireTrustedAttestation = boolean("require_trusted_attestation").bindTo { it.requireTrustedAttestation }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateReason = varchar("update_reason").bindTo { it.updateReason }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
