package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.Exist
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl
import org.hibernate.validator.internal.engine.path.MutablePath
import org.hibernate.validator.path.Path

/**
 * Exist constraint validator.
 *
 * @author K
 * @since 1.0.0
 */
class ExistValidator : ConstraintValidator<Exist, Any?> {
    private lateinit var exist: Exist

    override fun initialize(exist: Exist) {
        this.exist = exist
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }

        val constraints = exist.value
        val validator = ConstraintsValidator()
        validator.initialize(constraints)

        val hvContext = context as? ConstraintValidatorContextImpl
            ?: return validator.isValid(value, context)
        val path = hvContext.constraintViolationCreationContexts.first().path
        if (path is Path) {
            val propName = path.leafNode.name
            // Create a temporary context to avoid carrying sub-constraint error messages; the sub-constraint message is meaningless and the final error message comes from the parent Exist constraint's message.
            val tempContext = ConstraintValidatorContextImpl(
                hvContext.clockProvider,
                MutablePath.createPathFromString(propName),
                hvContext.constraintDescriptor,
                null,
                null,
                null
            )

            val pass = when (value) {
                is Array<*> -> value.any { validator.isValid(it, tempContext) }
                is BooleanArray -> value.any { validator.isValid(it, tempContext) }
                is ByteArray -> value.any { validator.isValid(it, tempContext) }
                is CharArray -> value.any { validator.isValid(it, tempContext) }
                is DoubleArray -> value.any { validator.isValid(it, tempContext) }
                is FloatArray -> value.any { validator.isValid(it, tempContext) }
                is IntArray -> value.any { validator.isValid(it, tempContext) }
                is LongArray -> value.any { validator.isValid(it, tempContext) }
                is ShortArray -> value.any { validator.isValid(it, tempContext) }
                is Collection<*> -> value.any { validator.isValid(it, tempContext) }
                is Map<*, *> -> value.values.any { validator.isValid(it, tempContext) }
                else -> validator.isValid(value, tempContext)
            }

            hvContext.disableDefaultConstraintViolation()
            hvContext.buildConstraintViolationWithTemplate(exist.message).addConstraintViolation()
            return pass
        } else {
            error("should never happen")
        }
    }

}
