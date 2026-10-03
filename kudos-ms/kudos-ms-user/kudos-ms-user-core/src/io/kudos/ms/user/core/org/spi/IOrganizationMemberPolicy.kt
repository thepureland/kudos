package io.kudos.ms.user.core.org.spi

/**
 * Who may write an organization's shared directory: departments and member accounts.
 *
 * The user module only proves that a row belongs to the caller's organization; whether this caller
 * may change it (organization administrator, organization permission administrator, …) is an
 * authorization fact owned by the auth module, which implements this port. User core never depends
 * on auth core.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IOrganizationMemberPolicy {

    /** Create, update, move, enable/disable or delete departments of [organizationId]. */
    fun assertCanManageDirectory(organizationId: String)

    /** Create member accounts or update their non-lifecycle attributes. */
    fun assertCanManageMember(organizationId: String, userId: String?)

    /**
     * End or suspend [userId]'s membership: disable, freeze or delete the account (G-16, G-17).
     * Implementations refuse ending the last organization administrator unless the platform does it.
     */
    fun assertCanEndMembership(organizationId: String, userId: String)

    /**
     * Whether organization account [userId], signing in through tenant [tenantId], gets a session at all:
     * it may enter that tenant now (G-4, G-9), or it holds an organization management identity and so
     * may work in the organization scope. Which of the two it gets is decided when the session is issued.
     */
    fun canSignIn(tenantId: String, userId: String): Boolean
}
