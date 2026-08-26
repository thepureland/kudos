package io.kudos.ms.auth.core.authentication.mfa.service.iservice

import io.kudos.ms.auth.common.authentication.vo.TotpEnrollmentChallenge

interface ITotpEnrollmentService {
    fun begin(userId: String, tenantId: String, accountName: String): TotpEnrollmentChallenge
    fun confirm(enrollmentId: String, userId: String, tenantId: String, code: Int): Boolean
    fun cancel(enrollmentId: String, userId: String, tenantId: String): Boolean
    fun isEnabled(userId: String, tenantId: String): Boolean
    fun disable(userId: String, tenantId: String): Boolean
}
