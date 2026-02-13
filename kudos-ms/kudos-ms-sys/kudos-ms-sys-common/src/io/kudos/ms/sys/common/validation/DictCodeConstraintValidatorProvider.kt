package io.kudos.ms.sys.common.validation

import io.kudos.base.bean.validation.constraint.annotations.DictCode
import io.kudos.context.validation.IConstraintValidatorProviderBean
import jakarta.validation.ConstraintValidator
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * DictCode约束验证器提供者
 *
 * @author K
 * @since 1.0.0
 */
@Component
class DictCodeConstraintValidatorProvider: IConstraintValidatorProviderBean {

    override fun <T : Annotation, V : ConstraintValidator<T, *>> provide(): Map<KClass<T>, KClass<V>> {
        @Suppress("UNCHECKED_CAST")
        return mapOf(DictCode::class to DictCodeValidator::class) as Map<KClass<T>, KClass<V>>
    }

}