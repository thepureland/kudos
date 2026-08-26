package io.kudos.ms.user.core.account.service.iservice

import io.kudos.ms.user.core.account.model.UserAccountThirdAuditEvent

interface IUserAccountThirdAuditService {
    fun record(event: UserAccountThirdAuditEvent): String
}
