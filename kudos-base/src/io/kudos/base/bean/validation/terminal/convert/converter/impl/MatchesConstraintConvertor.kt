package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.terminal.TerminalConstraint
import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertContext
import io.kudos.base.bean.validation.terminal.convert.converter.AbstractConstraintConvertor
import jakarta.validation.constraints.Pattern

/**
 * [Matches] -> terminal [Pattern] rule (regexp / message / flags).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class MatchesConstraintConvertor(annotation: Annotation) : AbstractConstraintConvertor(annotation) {

    override fun convert(context: ConstraintConvertContext): TerminalConstraint {
        val result = super.convert(context)
        return result.copy(constraint = Pattern::class.java.simpleName)
    }

    public override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        require(constraintAnnotation is Matches) {
            "MatchesConstraintConvertor only supports the Matches annotation"
        }
        val kind = constraintAnnotation.value
        val message = constraintAnnotation.message.ifBlank { kind.defaultMessageKey }
        return linkedMapOf(
            "regexp" to kind.regex,
            "message" to message,
            "flags" to 0,
        )
    }
}
