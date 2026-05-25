package io.kudos.context.init

import io.kudos.base.bean.validation.support.ValidationContext
import io.kudos.context.validation.CustomConstraintValidatorFactory
import jakarta.annotation.PostConstruct
import org.hibernate.validator.HibernateValidator
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Role
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

/**
 * 验证器配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class ValidatorAutoConfiguration : IComponentInitializer {

    /**
     * 项目级默认 Validator，使用 HibernateValidator 实现 + 自定义 [CustomConstraintValidatorFactory]。
     *
     * 通过 `@Primary` 让所有按 `Validator` / `LocalValidatorFactoryBean` 类型注入的地方都拿到此 Bean。
     *
     * 注意：不再用 `@Bean("mvcValidator")` 占名。Spring Boot 4 默认禁止 Bean 定义覆盖
     * (`spring.main.allow-bean-definition-overriding=false`)，而 `WebMvcAutoConfiguration$EnableWebMvcConfiguration`
     * 自身也会注册一个名为 `mvcValidator` 的 Bean，硬要同名会启动失败。
     *
     * Spring MVC 的 `mvcValidator` 槽位由 `kudos-ability-web-springmvc` 模块的
     * `SpringMvcAutoConfiguration.getValidator()` 钩入此 Bean —— 那是 Spring 官方推荐的替换方式。
     */
    companion object {
        /**
         * 返回 `BeanPostProcessor` 的工厂方法必须声明为 **static**（Kotlin 用 `companion object` + `@JvmStatic`），
         * 否则 Spring 强制提前实例化 `@Configuration` 自身才能取得 BPP，会使该配置类被排除在其它 BPP 的处理流程之外。
         *
         * 同时 `defaultValidator` 也放进 companion：它是 `methodValidationPostProcessor` 的构造参数，
         * 若仍是实例方法，BPP 的依赖解析会反向触发 `ValidatorAutoConfiguration` 早期实例化（warn 仍会出现）。
         */
        /**
         * `@Role(ROLE_INFRASTRUCTURE)` 告诉 Spring 这是基础设施 bean，不需要被业务 BeanPostProcessor 处理。
         * 没有这个标记时，由于它会被 BPP（methodValidationPostProcessor）作为构造参数依赖而提前实例化，
         * Spring 会发 "is not eligible for getting processed by all BeanPostProcessors" 警告。
         */
        @Bean
        @Primary
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        @JvmStatic
        fun defaultValidator(): LocalValidatorFactoryBean =
            CustomConstraintValidatorFactory().apply { setProviderClass(HibernateValidator::class.java) }

        @Bean
        @Primary
        @JvmStatic
        fun methodValidationPostProcessor(validator: LocalValidatorFactoryBean): MethodValidationPostProcessor =
            MethodValidationPostProcessor().apply { setValidator(validator) }
    }

    /**
     * 将 Spring 容器内创建的 validator 桥接给 [ValidationContext]，让 kudos-base 的
     * 验证工具（`ValidationKit` / 各自定义 `ConstraintValidator`）在容器外也能拿到
     * Spring 注入版本的 validator。
     *
     * 重构动因：之前 `ValidationContext.validator = validator` 写在 [defaultValidator]
     * 的 `@Bean` 工厂方法体里——属于"Bean 工厂方法有副作用"反模式。Bean 工厂方法应
     * **只返回新实例**，不动外部状态；副作用应放到生命周期回调里。
     *
     * 移到独立 bridge bean 的 [PostConstruct] 里，保证：
     * - 副作用与 Bean 构造解耦
     * - Bean 销毁/重建时副作用不被重复触发
     * - 时序明确：在 LocalValidatorFactoryBean 已完整初始化后才发生
     */
    @Bean
    open fun validationContextBridge(validator: LocalValidatorFactoryBean): ValidationContextBridge =
        ValidationContextBridge(validator)

    /** 桥接 bean。仅持有 validator 引用，PostConstruct 时把它写到 [ValidationContext]。 */
    class ValidationContextBridge(private val validator: LocalValidatorFactoryBean) {
        @PostConstruct
        fun bridgeToValidationContext() {
            ValidationContext.validator = validator
        }
    }

    override fun getComponentName() = "kudos-bean-validator"

}