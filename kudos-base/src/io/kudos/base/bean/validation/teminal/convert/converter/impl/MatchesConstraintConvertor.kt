package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.teminal.TeminalConstraint
import io.kudos.base.bean.validation.teminal.convert.ConstraintConvertContext
import io.kudos.base.bean.validation.teminal.convert.converter.AbstractConstraintConvertor
import jakarta.validation.constraints.Pattern

/**
 * [Matches] -> 终端 [Pattern] 规则（regexp / message / flags）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class MatchesConstraintConvertor(annotation: Annotation) : AbstractConstraintConvertor(annotation) {

    override fun convert(context: ConstraintConvertContext): TeminalConstraint {
        val result = super.convert(context)
        return result.copy(constraint = Pattern::class.java.simpleName)
    }

    public override fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any> {
        require(constraintAnnotation is Matches) {
            "MatchesConstraintConvertor 仅支持 Matches 注解"
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
