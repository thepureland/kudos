package io.kudos.base.bean.validation.terminal.convert.converter

import io.kudos.base.bean.validation.terminal.TerminalConstraint
import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertContext
import kotlin.reflect.full.declaredMemberProperties

/**
 * Abstract converter from annotation-based constraints to terminal constraints.
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractConstraintConvertor(protected var annotation: Annotation) : IConstraintConvertor {

    protected lateinit var context: ConstraintConvertContext

    private lateinit var constraintAnnotation: Annotation

    override fun convert(context: ConstraintConvertContext): TerminalConstraint {
        this.context = context
        val rules = this.handleRules()
        val constraint = requireNotNull(this.constraintAnnotation.annotationClass.simpleName) {
            "Unable to resolve constraint annotation name: ${this.constraintAnnotation.annotationClass}"
        }
        return TerminalConstraint(context.property, constraint, rules)
    }

    /**
     * Returns the rule for a specific constraint annotation.
     *
     * @param constraintAnnotation the specific constraint annotation (never the inner List annotation)
     * @return LinkedHashMap<annotationPropertyName, annotationPropertyValue>
     */
    protected abstract fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any>

    /**
     * Handles constraint annotations (which may be the constraint annotation itself or its inner List annotation).
     *
     * @return Array<Map<annotationPropertyName, annotationPropertyValue>>
     */
    private fun handleRules(): Array<Map<String, Any>> {
        val rules = mutableListOf<Map<String, Any>>()
        val annotationClass = requireNotNull(this.annotation.annotationClass.qualifiedName) {
            "Unable to resolve annotation qualified name: ${this.annotation.annotationClass}"
        } // may be the constraint annotation class or the constraint's List annotation class
        if (annotationClass.endsWith(".List")) {
            // For a List annotation wrapping specific constraint annotations, iterate and process each one
            val annotationsProp = annotation.annotationClass.declaredMemberProperties.first()
            val constraintAnnotations = annotationsProp.call(annotation) as? Array<*>
                ?: error("The value property of List annotation [$annotationClass] is not an array type")
            constraintAnnotations.forEach {
                val constraint = it as? Annotation
                    ?: error("List annotation [$annotationClass] contains a non-annotation element: $it")
                rules.add(handleRule(constraint))
            }
        } else {
            // A specific constraint annotation without a List annotation wrapper
            rules.add(handleRule(annotation))
        }
        return rules.toTypedArray()
    }

    /**
     * Handles the rule of a specific constraint annotation.
     *
     * @param constraintAnnotation the specific constraint annotation (never the inner List annotation)
     * @return Map<annotationPropertyName, annotationPropertyValue>
     */
    private fun handleRule(constraintAnnotation: Annotation): Map<String, Any> {
        this.constraintAnnotation = constraintAnnotation
        val rule = getRule(constraintAnnotation)
        handleMessageI18n(rule, constraintAnnotation)
        return rule
    }

    /**
     * Handles i18n of error messages.
     * Only when message is {@code {...}} and the content inside the braces starts with {@code jakarta.validation.constraints} or
     * {@code org.hibernate.validator.constraints}, the template returned by [getCustomDefaultMsgI18nKey] is substituted directly.
     *
     * @param rule the constraint rule (mutable); rule["message"] may be modified
     * @param constraintAnnotation the current constraint annotation
     */
    protected open fun handleMessageI18n(rule: MutableMap<String, Any>, constraintAnnotation: Annotation) {
        val raw = rule["message"] as? String ?: return
        if (raw.isEmpty()) return
        if (!raw.startsWith("{") || !raw.endsWith("}")) return
        val key = raw.drop(1).dropLast(1).trim()
        if (!key.startsWith("jakarta.validation.constraints") && !key.startsWith("org.hibernate.validator.constraints")) return
        val template = getCustomDefaultMsgI18nKey(constraintAnnotation)
        rule["message"] = template
    }

    /**
     * Returns the custom default i18n key for a third-party constraint annotation.
     *
     * @param constraintAnnotation the constraint annotation
     * @return the default i18n key string
     */
    protected open fun getCustomDefaultMsgI18nKey(constraintAnnotation: Annotation): String {
        return "sys.valid-msg.default.${constraintAnnotation.annotationClass.simpleName}"
    }

}
