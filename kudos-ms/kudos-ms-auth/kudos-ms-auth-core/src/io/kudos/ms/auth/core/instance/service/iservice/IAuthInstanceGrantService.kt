package io.kudos.ms.auth.core.instance.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.instance.vo.InstanceGrantVo
import io.kudos.ms.auth.core.instance.model.po.AuthInstanceGrant
import java.time.LocalDateTime


/**
 * Point-to-point sharing: grants on one particular instance of one resource.
 *
 * **Subject to the same admission rules as every other grant.** A share names a recipient and a
 * tenant, and both are checked against the principal directory rather than taken on trust — without
 * that, sharing is a way to hand access to a principal in another tenant, which is the one boundary
 * the whole model treats as hard.
 *
 * The complement to the role model rather than a competitor to it. Roles say what a *kind* of person
 * may do; this says that *this* person may act on *this* row. Both are evaluated by the same decision
 * algebra, so a DENY from either side still wins — a share can never be a way around a revocation.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthInstanceGrantService : IBaseCrudService<String, AuthInstanceGrant> {

    /**
     * Shares an instance with a principal, or updates the existing share for the same tuple.
     *
     * Update-not-duplicate is deliberate: sharing the same thing twice is something users do
     * routinely, and it should adjust the share rather than leave two rows whose combined meaning
     * nobody can predict.
     *
     * @param principalId who receives it
     * @param tenantId the tenant, the hard boundary as everywhere else
     * @param resourceType the kind of thing, e.g. `doc`
     * @param instanceId the particular one
     * @param action what may be done, as a permission code; `*` for anything on this instance
     * @param effect ALLOW or DENY
     * @param startTime effective from; NULL = immediately
     * @param endTime effective until; NULL = never expires
     * @param grantedBy who shared it
     * @param principalType what kind of principal the recipient is
     * @return the grant id
     * @throws IllegalArgumentException when the recipient does not exist, is disabled, or belongs to
     *   a different tenant than [tenantId] names
     */
    fun share(
        principalId: String,
        tenantId: String,
        resourceType: String,
        instanceId: String,
        action: String,
        effect: String = "ALLOW",
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        grantedBy: String? = null,
        principalType: String = "USER",
    ): String

    /**
     * Withdraws a share. Soft, like every other revocation here: an un-share must stay answerable
     * afterwards.
     *
     * @param grantId the share to withdraw
     * @param reason recorded on the row
     * @return whether a live share was withdrawn
     */
    fun unshare(grantId: String, reason: String? = null): Boolean

    /**
     * Everyone a particular instance is currently shared with — what a sharing dialog renders.
     *
     * @param resourceType the kind of thing
     * @param instanceId the particular one
     * @return the live shares on it
     */
    fun listShares(resourceType: String, instanceId: String): List<AuthInstanceGrant>

    /**
     * The same, projected for an API surface — principal names resolved, the validity window
     * evaluated into an `active` flag so the caller does not re-implement the comparison.
     *
     * @param resourceType the kind of thing
     * @param instanceId the particular one
     * @return the live shares on it
     */
    fun listShareVos(resourceType: String, instanceId: String): List<InstanceGrantVo>

    /**
     * Everything currently shared **with** a principal.
     *
     * The other direction, and it answers a question nothing else in the module can: instance shares
     * sit outside the role model on purpose, so "what has this person been given" is invisible to
     * every role report. An offboarding review that only walks roles misses exactly this.
     *
     * @param principalId the subject
     * @param resourceType narrow to one kind of thing, or null for all
     * @return the live shares held by the principal
     */
    fun listSharesOfPrincipal(principalId: String, resourceType: String? = null): List<InstanceGrantVo>
}
