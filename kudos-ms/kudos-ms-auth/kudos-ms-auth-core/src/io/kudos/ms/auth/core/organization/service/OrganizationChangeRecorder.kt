package io.kudos.ms.auth.core.organization.service

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.organization.dao.AuthOrganizationAuditDao
import io.kudos.ms.auth.core.organization.dao.AuthOrganizationRevisionDao
import io.kudos.ms.auth.core.organization.model.po.AuthOrganizationAudit
import io.kudos.ms.user.common.passport.CurrentUserKit
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/** An organization's authorization facts changed (committed with the change; listeners may notify). */
data class OrganizationAuthorizationChanged(
    val organizationId: String,
    val revision: Long,
    val action: String,
    val tenantId: String? = null,
    val targetUserId: String? = null,
)

/** The revision a writer expected no longer matches; the client re-reads and retries. */
class OrganizationRevisionConflictException(val currentRevision: Long) :
    IllegalStateException("AUTHZ_REVISION_CONFLICT:$currentRevision")

/**
 * The bookkeeping every organization authorization change shares: serialize on the organization's
 * revision row, check the caller's expected revision, bump it, write the audit record and publish the
 * change — all in the caller's transaction.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationChangeRecorder(
    private val revisions: AuthOrganizationRevisionDao,
    private val audits: AuthOrganizationAuditDao,
    private val events: ApplicationEventPublisher,
) {

    private val log = LogFactory.getLog(this::class)

    /**
     * Locks [organizationId] for the rest of the transaction. When [expectedRevision] is given it must
     * equal the current revision, so a client never overwrites changes it has not seen.
     */
    open fun begin(organizationId: String, expectedRevision: Long? = null): Long {
        val current = revisions.lock(organizationId)
        if (expectedRevision != null && expectedRevision != current) throw OrganizationRevisionConflictException(current)
        return current
    }

    open fun current(organizationId: String): Long = revisions.current(organizationId)

    /** Records one change; call after [begin] in the same transaction. Returns the new revision. */
    open fun commit(
        organizationId: String,
        action: String,
        tenantId: String? = null,
        targetUserId: String? = null,
        targetId: String? = null,
        detail: String? = null,
        reason: String? = null,
    ): Long {
        val revision = revisions.bump(organizationId)
        audits.insert(AuthOrganizationAudit {
            this.organizationId = organizationId
            this.actorId = CurrentUserKit.currentUserIdOrNull()
            this.action = action
            this.tenantId = tenantId
            this.targetUserId = targetUserId
            this.targetId = targetId
            this.detail = detail?.take(4000)
            this.reason = reason?.take(512)
            this.createTime = LocalDateTime.now()
        })
        log.info("Organization $organizationId: $action (tenant=$tenantId, user=$targetUserId, target=$targetId) → revision $revision.")
        events.publishEvent(OrganizationAuthorizationChanged(organizationId, revision, action, tenantId, targetUserId))
        return revision
    }
}
