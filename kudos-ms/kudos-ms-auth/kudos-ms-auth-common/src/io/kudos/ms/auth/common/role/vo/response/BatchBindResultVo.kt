package io.kudos.ms.auth.common.role.vo.response

import java.io.Serializable

/**
 * Result of a batch bind operation (role↔user or group↔user).
 *
 * Partial-success is expected behaviour: each owner's bind is a separate transaction so a
 * single bad row doesn't block the rest. The admin UI uses [failures] to render per-owner
 * error messages.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class BatchBindResultVo(
    /** Number of owners (roles or groups) whose bind succeeded. */
    val ok: Int,
    /** Per-owner failures. Empty when [ok] equals the request's owner count. */
    val failures: List<BatchBindFailure>,
) : Serializable {

    data class BatchBindFailure(
        /** Owner id (role id or group id) whose bind failed. */
        val ownerId: String,
        /** Human-readable failure reason — exception message, not stack trace. */
        val reason: String,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun empty(): BatchBindResultVo = BatchBindResultVo(ok = 0, failures = emptyList())
    }
}
