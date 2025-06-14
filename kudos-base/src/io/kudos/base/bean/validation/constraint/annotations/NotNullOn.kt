package io.kudos.base.bean.validation.constraint.annotations

import io.kudos.base.bean.validation.constraint.validator.NotNullOnValidator
import io.kudos.base.bean.validation.support.Depends
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * 非null依赖约束注解，属性级别注解。
 *
 *
 * 当前属性的值是否可以为null，取决于定义的表达式。表达式为false，属性值可为null，即非必填项；表达式为true，属性值必填。
 * 注意：此约束注解不可与 @NotNull 一起使用
 * 注意：不可类似地实现如 NotEmptyOn 和 NotBlankOn 约束注解。NotNullOn之所以可行是因为各约束注解在校验时值为null都返回true。
 *
 * @author K
 * @since 1.0.0
 */
@Constraint(validatedBy = [NotNullOnValidator::class])
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class NotNullOn(
    /**
     * NotNull约束依赖的前提条件, 条件成立等效于@NotNull，条件不成立时等效于没有此约束注解
     */
    val depends: Depends,
    /**
     * 校验不通过时的提示，或其国际化key。
     * 每个约束定义中都包含有一个用于提示验证结果的消息模版, 并且在声明一个约束条件的时候,你可以通过这个约束中的message属性来重写默认的消息模版,
     * 如果在校验的时候,这个约束条件没有通过,那么你配置的MessageInterpolator会被用来当成解析器来解析这个约束中定义的消息模版,
     * 从而得到最终的验证失败提示信息. 这个解析器会尝试解析模版中的占位符( 大括号括起来的字符串 ).
     * 其中, Hibernate Validator中默认的解析器 (MessageInterpolator) 会先在类路径下找名称为ValidationMessages.properties的ResourceBundle,
     * 然后将占位符和这个文件中定义的resource进行匹配,如果匹配不成功的话,那么它会继续匹配Hibernate Validator自带的位于
     * /org/hibernate/validator/ValidationMessages.properties的ResourceBundle, 依次类推,递归的匹配所有的占位符.
     */
    val message: String = "{io.kudos.base.bean.validation.constraint.annotations.NotNullOn.message}",
    /**
     * 该校验规则所从属的分组类，通过分组可以过滤校验规则或排序校验顺序。默认值必须是空数组。
     * 校验组能够让你在验证的时候选择应用哪些约束条件. 这样在某些情况下( 例如向导 ) 就可以对每一步进行校验的时候, 选取对应这步的那些约束条件进行验证了.
     * 校验组是通过可变参数传递给validate, validateProperty 和 validateValue的.如果某个约束条件属于多个组,那么各个组在校验时候的顺序是不可预知的.
     * 如果一个约束条件没有被指明属于哪个组,那么它就会被归类到默认组(jakarta.validation.groups.Default).
     *
     * @GroupSequence 定义组别之间校验的顺序，使用注意事项：
     * 1.作用于类上时,不能包含jakarta.validation.groups.Default::class分组，作用于接口上可以
     * 2.作用于类上时,不能没有待验证的Bean的Class的分组
     * @GroupSequenceProvider 根据对象状态动态重定义默认分组，实现类返回的分组必须包含待验证的Bean的Class的分组(因为如果`Default`组对T进行验证，
     * 则实际验证的实例将传递给此类以确定默认组序列)。
     * 注：在使用组序列验证的时候，如果序列前边的组验证失败，则后面的组将不再给予验证！
     * 注：同一分组间的约束校验是无序的
     */
    val groups: Array<KClass<*>> = [],
    /**
     * 约束注解的有效负载(通常用来将一些元数据信息与该约束注解相关联，常用的一种情况是用负载表示验证结果的严重程度)
     */
    val payload: Array<KClass<out Payload>> = []
)
