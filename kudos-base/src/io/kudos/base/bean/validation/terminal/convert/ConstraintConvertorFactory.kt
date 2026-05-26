package io.kudos.base.bean.validation.terminal.convert

import io.kudos.base.bean.validation.constraint.annotations.*
import io.kudos.base.bean.validation.terminal.convert.converter.IConstraintConvertor
import io.kudos.base.bean.validation.terminal.convert.converter.impl.*


/**
 * Factory of constraint-annotation to terminal-constraint converters.
 *
 * @author K
 * @since 1.0.0
 */
object ConstraintConvertorFactory {

    /**
     * Returns the terminal-constraint converter for the given constraint annotation.
     *
     * @param annotation the constraint annotation
     * @return the terminal-constraint converter
     * @author K
     * @since 1.0.0
     */
    fun getInstance(annotation: Annotation): IConstraintConvertor? =
        when (annotation.annotationClass) {
            DictItemCode::class -> null // null entries don't need to be returned to the terminal
            DictEnumItemCode::class -> DictEnumCodeConstraintConvertor(annotation)
            Compare::class, Compare.List::class -> CompareConstraintConvertor(annotation)
            NotNullOn::class -> NotNullOnConstraintConvertor(annotation)
            Each::class -> EachConstraintConvertor(annotation)
            Exist::class -> ExistConstraintConvertor(annotation)
            Constraints::class -> ConstraintsConstraintConvertor(annotation)
            Remote::class -> RemoteConstraintConvertor(annotation)
            Matches::class -> MatchesConstraintConvertor(annotation)
            else -> DefaultConstraintConvertor(annotation)
        }

}
