package io.kudos.ms.auth.core.authentication.mfa.recovery.service.iservice

import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeSet
import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeStatus

interface IRecoveryCodeService {
    fun generate(userId: String, tenantId: String): RecoveryCodeSet
    fun status(userId: String, tenantId: String): RecoveryCodeStatus
    fun consume(userId: String, tenantId: String, rawCode: String): Boolean
    fun revoke(userId: String, tenantId: String): Boolean
}
