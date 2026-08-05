package io.kudos.ms.auth.common.role.vo

import java.io.Serializable


/**
 * One code-addressed permission binding on a role, as the admin UI sees it.
 *
 * Deliberately separate from the resource-tree view: a resource binding is a checkbox on a node,
 * while this is a row with three independent decisions (which code, ALLOW or DENY, under what
 * condition). Rendering them as one control would force the tree to grow a per-node effect and
 * condition editor, which is the wrong shape for the 99% of nodes that are a plain ALLOW.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class PermissionBindingVo(

    /** Binding id, for editing and removal. */
    val id: String,

    /** The role it belongs to. */
    val roleId: String,

    /** The semantic code, wildcards included (`sys:user:*`). */
    val permissionCode: String,

    /** ALLOW or DENY. */
    val effect: String,

    /** Optional condition expression; NULL = unconditional. */
    val condition: String? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}


/**
 * Create-or-update request for a code-addressed binding.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class PermissionBindingRequest(

    /** The role gaining (or losing) the permission. */
    val roleId: String,

    /** The semantic code; wildcards allowed. */
    val permissionCode: String,

    /** ALLOW or DENY; anything unrecognised is normalised to ALLOW rather than guessed at. */
    val effect: String = "ALLOW",

    /** Optional condition expression. */
    val condition: String? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
