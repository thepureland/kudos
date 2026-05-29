package io.kudos.ms.auth.common.role.vo.request

import java.io.Serializable

/**
 * Request body for atomic role-copy.
 *
 * The server reads the source role's full detail, builds a new role with [code] / [name]
 * overridden (the rest inherited from source), inserts it, and — when [copyResources] is true —
 * binds the source's resource grants to the new role in the same transaction.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleCopyRequest(
    /** Role to copy from. */
    val sourceId: String,
    /** New role's code; must be unique under the same tenant + subsystem. */
    val code: String,
    /** New role's name. */
    val name: String,
    /** When true, the source's resource grants are copied to the new role in the same transaction. */
    val copyResources: Boolean = true,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
