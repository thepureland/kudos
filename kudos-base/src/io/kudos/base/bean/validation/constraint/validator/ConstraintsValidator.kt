package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.support.ValidationContext
import io.kudos.base.bean.validation.support.ValidatorFactory
import io.kudos.base.lang.reflect.getMemberProperty
import io.kudos.base.lang.reflect.getMemberPropertyValue
import io.kudos.base.support.Consts
import io.kudos.base.support.logic.AndOrEnum
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Null
import kotlin.reflect.full.declaredMemberProperties

/**
 * Constraints约束验证器
 *
 * @author K
 * @since 1.0.0
 */
class ConstraintsValidator : ConstraintValidator<Constraints, Any?> {

    private lateinit var constraints: Constraints

    override fun initialize(constraints: Constraints) {
        this.constraints = constraints
    }

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        val annotations = getAnnotations(constraints)
        val bean = ValidationContext.get(context)
        if (constraints.andOr == AndOrEnum.AND) {
            val failFast = ValidationContext.isFailFast()
            var pass = true
            annotations.forEach {
                pass = validate(it, value, bean, context)
                if (!pass) {
                    addViolation(context, it)
                    if (failFast) {
                        return false
                    }
                }
            }
            return pass
        } else { // 有一个约束成功就算通过，并且不受failFast影响
            annotations.forEach {
                val pass = validate(it, value, bean, context)
                if (pass) {
                    return true
                }
            }
            return false
        }
    }

    companion object {
        fun getAnnotations(constraints: Constraints): List<Annotation> {
            // 获取定义的子约束
            val annotations = mutableListOf<Annotation>()
            var priorityAnnotation: Annotation? = null // 强制优先NotBlank、NotEmpty、NotNull、Null之一
            constraints.annotationClass.declaredMemberProperties.forEach {
                if (it.name != "order" && it.name != "andOr" && it.name != "message" && it.name != "groups" && it.name != "payload") {
                    val annotation = it.call(constraints) as Annotation
                    val message = annotation.annotationClass.getMemberPropertyValue(annotation, "message")
                    if (message != Constraints.MESSAGE) {
                        annotations.add(annotation)
                        if (annotation.annotationClass in setOf(
                                NotBlank::class,
                                NotEmpty::class,
                                NotNull::class,
                                Null::class
                            )
                        ) {
                            priorityAnnotation = annotation
                        }
                    }
                }
            }

            // 根据指定的顺序排序子约束
            val result = if (constraints.order.isNotEmpty()) {
                val sequenceAnnotations = linkedSetOf<Annotation>() // 有指定顺序的子约束
                constraints.order.forEach { clazz ->
                    val annotation = annotations.firstOrNull { it.annotationClass == clazz }
                    if (annotation == null) {
                        error("Constraints约束sequence中指定的【$clazz】子约束未定义规则！")
                    } else {
                        sequenceAnnotations.add(annotation)
                    }
                }
                sequenceAnnotations.addAll(annotations) // 合并未指定顺序的子约束
                sequenceAnnotations.toList()
            } else {
                annotations
            }

            // 强制优先NotBlank、NotEmpty、NotNull、Null
            return if (priorityAnnotation != null) {
                val sequenceAnnotations = linkedSetOf<Annotation>()
                sequenceAnnotations.add(priorityAnnotation)
                sequenceAnnotations.addAll(result)
                sequenceAnnotations.toList()
            } else {
                result
            }
        }
    }

    private fun validate(
        annotation: Annotation,
        value: Any?,
        bean: Any?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (value == null) {
            return annotation.annotationClass != NotNull::class && annotation.annotationClass != NotEmpty::class
                    && annotation.annotationClass != NotBlank::class
        }

        val v = if (annotation is AtLeast) bean!! else value
        val validators = ValidatorFactory.getValidator(annotation, v)
        if (validators.isEmpty()) {
            error("Constraints约束不支持【${annotation.annotationClass}】作为其子约束！")
        } else {
            var pass = true
            validators.forEach {
                pass = pass && doValidate(it, annotation, v, context)
            }
            return pass
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun doValidate(
        validator: Any, annotation: Annotation, value: Any?, context: ConstraintValidatorContext
    ): Boolean =
        //!!! 强制转换不能去掉，否则会StackOverflow
        with(validator as ConstraintValidator<Annotation, Any?>) { isValid(value, context) }

    private fun addViolation(context: ConstraintValidatorContext, annotation: Annotation) {
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(
            annotation.annotationClass.getMemberProperty("message").call(annotation) as String
        ).addConstraintViolation()
    }

}
