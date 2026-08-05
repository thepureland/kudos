package io.kudos.ms.auth.common.authz.vo

import io.kudos.ms.auth.common.authz.enums.PermissionEffect
import java.io.Serializable


/**
 * Who is asking.
 *
 * Machine principals (service accounts, API keys, scheduled jobs) go through the same authorization
 * model as people — the decision point never needs to know which it is dealing with, only that a
 * principal has an id.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class SubjectRef(

    /** Principal id. */
    val principalId: String,

    /** Subject kind, matching `auth_role_user.principal_type`. */
    val principalType: String = "USER",

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        /** The common case: a human account id. */
        @JvmStatic
        fun ofUser(userId: String): SubjectRef = SubjectRef(userId)
    }
}


/**
 * One authorization question: *may this subject perform this action?*
 *
 * [permissionCode] is the whole question — the `域:资源类型:动作` code identifies both the action and
 * what it acts on. [context] carries the attributes a binding's condition may be evaluated against
 * (client ip, time, tenant, whatever the deployment registers); it is untyped on purpose so adding
 * an attribute never changes this contract.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthzRequest(

    /** Who is asking. */
    val subject: SubjectRef,

    /** The concrete permission code being requested; wildcards here are literal, see PermissionCodes. */
    val permissionCode: String,

    /** Attributes available to condition evaluation. */
    val context: Map<String, Any?> = emptyMap(),

    /**
     * The kind of thing being acted on, when the question is about a **particular** one
     * (`doc`, `order`). Together with [instanceId] this turns "may they read documents" into
     * "may they read *this* document", which role-derived grants cannot express.
     */
    val resourceType: String? = null,

    /** The particular instance. Only consulted when [resourceType] is also set. */
    val instanceId: String? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}


/**
 * The answer, plus **why** — the evidence is not a debugging extra, it is what makes the system
 * explainable. The same structure feeds the audit trail and the "why does this user have / not have
 * X" endpoint, so an operator never has to reconstruct a decision by reading code.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthzDecision(

    /** Whether the action is permitted. */
    val permitted: Boolean,

    /** Which rule settled it. */
    val reason: Reason,

    /** The permission code of the binding that settled it, when one did. */
    val matchedCode: String? = null,

    /** The role the deciding binding came from, when one did. */
    val matchedRoleId: String? = null,

    /** Free-text detail for the audit log / explain endpoint. */
    val detail: String? = null,

) : Serializable {

    /**
     * The closed set of ways a decision can be reached. Closed on purpose: an authorization outcome
     * that cannot be classified is an outcome nobody can audit.
     */
    enum class Reason {

        /** The subject is a platform-level administrator and short-circuits the algebra. */
        PLATFORM_ADMIN,

        /** An ALLOW binding matched and no DENY did. */
        ALLOWED_BY_GRANT,

        /** A grant on this particular instance permitted it — a share, not a role. */
        ALLOWED_BY_INSTANCE_GRANT,

        /** A DENY binding matched; it wins over any ALLOW. */
        DENIED_BY_RULE,

        /** Nothing matched. The default, and the reason an unregistered permission is refused. */
        DENIED_BY_DEFAULT,
    }

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun permit(reason: Reason, code: String? = null, roleId: String? = null, detail: String? = null) =
            AuthzDecision(true, reason, code, roleId, detail)

        @JvmStatic
        fun deny(reason: Reason, code: String? = null, roleId: String? = null, detail: String? = null) =
            AuthzDecision(false, reason, code, roleId, detail)
    }
}


/**
 * One resolved permission binding held by a subject: the flattened form of
 * `auth_role_resource` after roles, groups and the parent chain have been expanded.
 *
 * This is what the decision cache stores per subject — resolving it is the expensive part, and it is
 * done once per subject rather than once per question.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class PermissionGrantVo(

    /** The permission code this binding addresses. */
    val permissionCode: String,

    /** ALLOW or DENY. */
    val effect: PermissionEffect,

    /** Optional condition expression; NULL = unconditional. */
    val condition: String? = null,

    /** The role the binding came from — kept for the decision evidence, not for matching. */
    val roleId: String? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
