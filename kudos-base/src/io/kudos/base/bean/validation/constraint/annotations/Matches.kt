package io.kudos.base.bean.validation.constraint.annotations

import io.kudos.base.bean.validation.constraint.validator.MatchesValidator
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * 仅支持框架内置 [RegExpEnum] 与 [io.kudos.base.bean.validation.support.RegExps] 的分类正则；
 * 行为与 [jakarta.validation.constraints.Pattern] 一致（含 null 视为合法，与 [NotBlank] 等组合使用）。
 * 业务自定义规则请使用 [@Pattern][jakarta.validation.constraints.Pattern] 并引用 [io.kudos.base.bean.validation.support.RegExps] 中的常量。
 * 终端约束通过 [io.kudos.base.bean.validation.terminal.convert.converter.impl.MatchesConstraintConvertor] 转为 `Pattern` 的规则描述。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Constraint(validatedBy = [MatchesValidator::class])
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Matches(
    /**
     * 内置正则种类（与 [RegExps][io.kudos.base.bean.validation.support.RegExps] 一一对应）。
     */
    val value: RegExpEnum,
    /**
     * 校验不通过时的提示或其国际化 key；为空时使用 [RegExpEnum.defaultMessageKey]。
     */
    val message: String = "",
    /**
     * 该校验规则所从属的分组类
     */
    val groups: Array<KClass<*>> = [],
    /**
     * 约束注解的有效负载
     */
    val payload: Array<KClass<out Payload>> = [],
)
