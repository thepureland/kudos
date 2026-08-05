package io.kudos.ms.auth.common.instance.vo

import java.io.Serializable
import java.time.LocalDateTime


/**
 * One share of one instance, as the API surface renders it.
 *
 * A VO rather than the entity because a share is shown to people — a sharing dialog, an audit
 * screen — and those want the principal's name, not only its id.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class InstanceGrantVo(

    /** Grant id. */
    val id: String,

    /** Who holds it. */
    val principalId: String,

    /** Their display name, when resolvable. */
    val principalName: String? = null,

    /** Subject kind: USER / SERVICE / API_KEY. */
    val principalType: String? = null,

    /** The kind of thing, e.g. `doc`. */
    val resourceType: String,

    /** The particular one. */
    val instanceId: String,

    /** What may be done, as a permission code; `*` means anything on this instance. */
    val action: String,

    /** ALLOW or DENY. */
    val effect: String,

    /** Effective from; NULL = immediately. */
    val startTime: LocalDateTime? = null,

    /** Effective until; NULL = never expires. */
    val endTime: LocalDateTime? = null,

    /** Who shared it. */
    val grantedBy: String? = null,

    /** Whether the share is in force right now — the window evaluated for the caller. */
    val active: Boolean = true,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}


/**
 * A request to share one instance with one principal.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class InstanceShareRequest(

    /** Who receives it. */
    val principalId: String,

    /**
     * What kind of principal that is. Defaults to USER, and is checked against the directory —
     * a share to a service account resolves through that account's own directory rather than
     * being silently recorded as a person.
     */
    val principalType: String = "USER",

    /**
     * The tenant. Validated against the recipient's real tenant rather than trusted: the whole
     * model treats this boundary as hard, and a share that could name any tenant would be the way
     * through it.
     */
    val tenantId: String,

    /** The kind of thing, e.g. `doc`. */
    val resourceType: String,

    /** The particular one. */
    val instanceId: String,

    /**
     * What may be done, as a permission code. `*` grants anything on this instance — which is the
     * shape an "owner" share takes, and is bounded by the instance rather than being a wildcard over
     * the deployment.
     */
    val action: String,

    /** ALLOW or DENY; a DENY here withholds an action the role model would otherwise permit. */
    val effect: String = "ALLOW",

    /** Effective from; NULL = immediately. */
    val startTime: LocalDateTime? = null,

    /** Effective until; NULL = never expires. */
    val endTime: LocalDateTime? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
