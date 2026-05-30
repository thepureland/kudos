package io.kudos.ms.auth.common.grant.enums

/**
 * Lifecycle states of a role-grant approval request.
 *
 * Transitions (enforced by AuthRoleGrantRequestService):
 *   PENDING → APPROVED  (approve, performs the bind)
 *   PENDING → REJECTED  (reject)
 *   PENDING → CANCELLED (requester cancels)
 * Terminal states are immutable.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
enum class GrantRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    ;

    /** A request can only be acted on (approve/reject/cancel) while it is still pending. */
    fun isTerminal(): Boolean = this != PENDING

    companion object {
        /** Parse a wire string into the enum, or null if unrecognised. */
        @JvmStatic
        fun fromString(raw: String?): GrantRequestStatus? =
            raw?.let { v -> entries.firstOrNull { it.name == v.trim().uppercase() } }
    }
}
