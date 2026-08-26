package io.kudos.ms.auth.core.authentication.securityevent.service.iservice

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAcknowledgeCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAssignCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventCloseCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventSummary

interface IAuthSecurityEventService {
    fun recordOrAggregate(command: AuthSecurityEventRecordCommand)

    fun aggregateAfterConcurrentInsert(command: AuthSecurityEventRecordCommand)

    fun acknowledge(command: AuthSecurityEventAcknowledgeCommand): AuthSecurityEventSummary

    fun close(command: AuthSecurityEventCloseCommand): AuthSecurityEventSummary

    fun assign(command: AuthSecurityEventAssignCommand): AuthSecurityEventSummary

    fun listRecent(
        tenantId: String,
        userId: String? = null,
        riskLevel: String? = null,
        status: String? = null,
        assigneeUserId: String? = null,
        overdueOnly: Boolean = false,
        limit: Int = 100,
    ): List<AuthSecurityEventSummary>
}
