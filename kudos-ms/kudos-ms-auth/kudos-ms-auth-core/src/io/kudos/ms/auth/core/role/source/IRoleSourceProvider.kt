package io.kudos.ms.auth.core.role.source


/**
 * An additional source of roles, beyond kudos' own direct grants and group inheritance.
 *
 * **The business shape this exists for.** Plenty of organisations do not assign roles; they assign
 * *positions*, and the roles follow. An HR system says "this person is a regional approver" and the
 * permissions are a consequence — nobody ever wrote an `auth_role_user` row, and nobody should have
 * to, because the moment they do there are two records of the same fact and they drift.
 *
 * **Why it is a provider rather than a sync job.** The obvious alternative is to mirror the external
 * source into `auth_role_user` on a schedule. That fails in the direction that matters: between the
 * position ending and the next sync, the permissions are still live, and the gap is invisible.
 * Resolving through the source makes the external system's answer *the* answer, with no window.
 *
 * **Contract.**
 *  - [roleIdsOf] is on the permission-resolution path and must be cheap — read a cache or an indexed
 *    column, never a remote call. The result is cached by kudos, so it is not called per request,
 *    but it *is* called on every cache miss.
 *  - It contributes roles; it can never take one away. A provider returning nothing is a provider
 *    with nothing to add, not a veto — withholding permission is what a DENY binding is for, and
 *    letting a source silently subtract would make the decision depend on evaluation order.
 *  - The returned roles are subject to every other rule unchanged: deactivated roles still
 *    contribute nothing, the parent chain still expands, and tenant boundaries still hold.
 *
 * **Keeping it fresh.** kudos cannot observe writes to a table it does not know about, so when the
 * external fact changes the application must say so by publishing
 * [io.kudos.ms.auth.core.role.source.ExternalRoleSourceChanged]. Without that, the change lands
 * whenever the affected principal's cache next happens to be invalidated — which may be never.
 *
 * ```kotlin
 * @Component
 * class PositionRoleSource(private val positions: PositionDao, private val roles: IAuthRoleService)
 *     : IRoleSourceProvider {
 *     override fun sourceName() = "position"
 *     override fun roleIdsOf(principalId: String): Set<String> =
 *         positions.currentPositionsOf(principalId).mapNotNull { roles.getIdByCode(it.roleCode) }.toSet()
 * }
 * ```
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IRoleSourceProvider {

    /** Names this source in explanations and logs — `position`, `orgAutoRole`, and so on. */
    fun sourceName(): String

    /**
     * The role **ids** this source says the principal holds.
     *
     * Ids rather than codes on purpose: codes are tenant-scoped, so resolving them here would mean
     * working out the principal's tenant first and silently dropping whatever failed to resolve. A
     * provider that thinks in codes converts them itself, where a miss is its own to notice.
     *
     * @param principalId the principal
     * @return the contributed role ids; empty when this source has nothing to say about them
     */
    fun roleIdsOf(principalId: String): Set<String>
}


/**
 * Published by an application when an external role source's answer changed for some principals.
 *
 * kudos has no way to observe a table it does not know about, so this is the only thing that can
 * make the change take effect promptly.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class ExternalRoleSourceChanged(

    /** The principals whose contributed roles may now differ. */
    val principalIds: Collection<String>,

    /** Which source changed, for the log line. */
    val sourceName: String? = null,
)
