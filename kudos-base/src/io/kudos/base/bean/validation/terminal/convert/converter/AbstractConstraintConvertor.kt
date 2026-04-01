package io.kudos.base.bean.validation.terminal.convert.converter

import io.kudos.base.bean.validation.terminal.TerminalConstraint
import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertContext
import kotlin.reflect.full.declaredMemberProperties

/**
 * 抽象的注解约束->终端约束的转换器
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
            "无法解析约束注解名称: ${this.constraintAnnotation.annotationClass}"
        }
        return TerminalConstraint(context.property, constraint, rules)
    }

    /**
     * 返回具体约束注解的规则
     *
     * @param constraintAnnotation 具体约束注解(不会是其内部注解List)
     * @return LinkedHashMap<注解属性名 ， 注解属性值>
     */
    protected abstract fun getRule(constraintAnnotation: Annotation): LinkedHashMap<String, Any>

    /**
     * 处理约束注解（可能是约束注解，也可能是约束的内部注解List）
     *
     * @return Array<Map> < 注解属性名 ， 注解属性值>>
     */
    private fun handleRules(): Array<Map<String, Any>> {
        val rules = mutableListOf<Map<String, Any>>()
        val annotationClass = requireNotNull(this.annotation.annotationClass.qualifiedName) {
            "无法解析注解限定名: ${this.annotation.annotationClass}"
        } // 可能是约束注解类，也可能是约束的List注解类
        if (annotationClass.endsWith(".List")) {
            // 为List注解包装具体约束注解的形式，遍历处理每一个具体约束注解
            val annotationsProp = annotation.annotationClass.declaredMemberProperties.first()
            val constraintAnnotations = annotationsProp.call(annotation) as? Array<*>
                ?: error("List 注解【$annotationClass】的 value 属性不是数组类型")
            constraintAnnotations.forEach {
                val constraint = it as? Annotation
                    ?: error("List 注解【$annotationClass】包含非注解元素: $it")
                rules.add(handleRule(constraint))
            }
        } else {
            // 为具体约束注解，没有其List注解包装
            rules.add(handleRule(annotation))
        }
        return rules.toTypedArray()
    }

    /**
     * 处理具体约束注解的规则
     *
     * @param constraintAnnotation 具体约束注解(不会是其内部注解List)
     * @return Map<注解属性名 ， 注解属性值>
     */
    private fun handleRule(constraintAnnotation: Annotation): Map<String, Any> {
        this.constraintAnnotation = constraintAnnotation
        val rule = getRule(constraintAnnotation)
        handleMessageI18n(rule, constraintAnnotation)
        return rule
    }

    /**
     * 处理错误消息的国际化。
     * 仅当 message 为 {@code {...}} 且花括号内以 {@code jakarta.validation.constraints} 或
     * {@code org.hibernate.validator.constraints} 开头时，用 [getCustomDefaultMsgI18nKey] 返回的模板直接替换。
     *
     * @param rule 约束规则（可变），可能被修改 rule["message"]
     * @param constraintAnnotation 当前约束注解
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
     * 获取第三方约束注解的自定义默认国际化key
     *
     * @param constraintAnnotation 约束注解
     * @return 默认国际化key字符串
     */
    protected open fun getCustomDefaultMsgI18nKey(constraintAnnotation: Annotation): String {
        return "sys.valid-msg.default.${constraintAnnotation.annotationClass.simpleName}"
    }

}
