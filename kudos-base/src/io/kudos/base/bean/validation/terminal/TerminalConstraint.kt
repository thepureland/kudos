package io.kudos.base.bean.validation.terminal

/**
 * Terminal constraint data class.
 *
 * @author K
 * @since 1.0.0
 */
data class TerminalConstraint(

    /** Bean property name */
//    @JsonIgnore
    @Transient
    val prop: String,
    /** Constraint name */
    val constraint: String,
    /** Constraint rule: Array(Map(constraint annotation attribute name, constraint annotation attribute value)) */
    val rule: Array<Map<String, Any>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherConstraint = other as? TerminalConstraint ?: return false

        if (prop != otherConstraint.prop) return false
        if (constraint != otherConstraint.constraint) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prop.hashCode()
        result = 31 * result + constraint.hashCode()
        return result
    }

}
