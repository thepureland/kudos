package io.kudos.base.bean.validation.terminal.convert.converter

import io.kudos.base.bean.validation.terminal.TerminalConstraint
import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertContext


/**
 * Constraint converter interface; responsible for converting annotation-based constraints into terminal constraints.
 *
 * @author K
 * @since 1.0.0
 */
interface IConstraintConvertor {

    /**
     * Converts an annotation-based constraint into a terminal constraint.
     *
     * @param context the context
     * @return the terminal constraint
     * @author K
     * @since 1.0.0
     */
    fun convert(context: ConstraintConvertContext): TerminalConstraint

}
