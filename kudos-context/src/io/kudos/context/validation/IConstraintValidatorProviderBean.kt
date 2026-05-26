package io.kudos.context.validation

import jakarta.validation.ConstraintValidator
import kotlin.reflect.KClass

/**
 * Constraint validator provider that associates constraint annotations with their validators. Implementations must be Spring beans.
 *
 * @author K
 * @since 1.0.0
 */
interface IConstraintValidatorProviderBean {

    /**
     * Provide constraint annotations and the validators to associate with them.
     *
     * @return Map(constraint class, validator class)
     * @author K
     * @since 1.0.0
     */
    fun <T: Annotation, V: ConstraintValidator<T, *>> provide(): Map<KClass<T>, KClass<V>>

}