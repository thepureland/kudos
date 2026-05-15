package io.kudos.context.init

import io.kudos.base.bean.validation.support.ValidationContext
import io.kudos.context.validation.CustomConstraintValidatorFactory
import jakarta.annotation.PostConstruct
import org.hibernate.validator.HibernateValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
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
     * 注册 [Spring MVC 命名约定](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/validation.html)
     * 的 "mvcValidator" Bean，并 `@Primary` 让所有按 `Validator` / `LocalValidatorFactoryBean`
     * 类型注入的地方都拿到我们的 [CustomConstraintValidatorFactory]。
     *
     * 之前同时标了 `@Primary` + `@Qualifier("mvcValidator")`，Spring 推荐二选一：
     * - `@Primary` 表达"按类型注入时我是首选"
     * - `@Qualifier` 表达"我有特定名字"
     * 两者同用语义冲突。用 `@Bean("mvcValidator") + @Primary` 既给名字又设为 primary，更清晰。
     */
    @Bean("mvcValidator")
    @Primary
    open fun defaultValidator(): LocalValidatorFactoryBean {
        val validator = CustomConstraintValidatorFactory()
        validator.setProviderClass(HibernateValidator::class.java)
        return validator
    }

    @Bean
    @Primary
    open fun methodValidationPostProcessor(validator: LocalValidatorFactoryBean): MethodValidationPostProcessor? {
        val processor = MethodValidationPostProcessor()
        processor.setValidator(validator)
        return processor
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