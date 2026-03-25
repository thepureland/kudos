package io.kudos.base.bean.validation.constraint.annotations

import jakarta.validation.Constraint
import jakarta.validation.OverridesAttribute
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import org.hibernate.validator.constraints.Length
import kotlin.reflect.KClass

/**
 * 最大长度约束，等价于只指定了 max 的 [Length]。
 *
 * 被校验对象类型必须为 CharSequence 或其子类。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@MustBeDocumented
@Constraint(validatedBy = [])
@Length
@ReportAsSingleViolation
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class MaxLength(
    /**
     * 最大长度
     */
    @get:OverridesAttribute(constraint = Length::class, name = "max")
    val max: Int,
    /**
     * 校验不通过时的提示
     */
    val message: String = "sys.valid-msg.default.MaxLength",
    /**
     * 该校验规则所从属的分组类，通过分组可以过滤校验规则或排序校验顺序。默认值必须是空数组。
     */
    val groups: Array<KClass<*>> = [],
    /**
     * 约束注解的有效负载(通常用来将一些元数据信息与该约束注解相关联，常用的一种情况是用负载表示验证结果的严重程度)
     */
    val payload: Array<KClass<out Payload>> = []
)
