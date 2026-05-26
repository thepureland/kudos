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
 * Validator configuration class.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class ValidatorAutoConfiguration : IComponentInitializer {

    /**
     * Project-level default Validator: HibernateValidator implementation + custom [CustomConstraintValidatorFactory].
     *
     * `@Primary` ensures every injection point typed as `Validator` / `LocalValidatorFactoryBean` resolves to this bean.
     *
     * Note: no longer using `@Bean("mvcValidator")` to claim that name. Spring Boot 4 disables bean-definition overriding by default
     * (`spring.main.allow-bean-definition-overriding=false`), and `WebMvcAutoConfiguration$EnableWebMvcConfiguration`
     * itself registers a bean named `mvcValidator`, so forcing the same name would fail startup.
     *
     * Spring MVC's `mvcValidator` slot is wired to this bean by `kudos-ability-web-springmvc`'s
     * `SpringMvcAutoConfiguration.getValidator()` — Spring's officially recommended replacement approach.
     */
    companion object {
        /**
         * Factory methods returning a `BeanPostProcessor` must be declared **static** (in Kotlin, via `companion object` + `@JvmStatic`);
         * otherwise Spring forces early instantiation of the `@Configuration` class itself to obtain the BPP, excluding the configuration class
         * from processing by other BPPs.
         *
         * `defaultValidator` is also placed in the companion: it is a constructor argument of `methodValidationPostProcessor`, so leaving it as an instance method
         * would cause the BPP's dependency resolution to reverse-trigger early instantiation of `ValidatorAutoConfiguration` (the warning would still appear).
         */
        /**
         * `@Role(ROLE_INFRASTRUCTURE)` tells Spring this is an infrastructure bean that does not need to go through business BeanPostProcessors.
         * Without this marker, because it is pulled in as a constructor-argument dependency by the BPP (methodValidationPostProcessor) and instantiated early,
         * Spring emits the warning "is not eligible for getting processed by all BeanPostProcessors".
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
     * Bridges the validator created inside the Spring container to [ValidationContext], so kudos-base's
     * validation utilities (`ValidationKit` / various custom `ConstraintValidator`s) can obtain the
     * Spring-injected validator outside the container too.
     *
     * Refactoring motivation: previously `ValidationContext.validator = validator` was written inside
     * the `@Bean` factory method body of [defaultValidator] — the "Bean factory methods with side effects" anti-pattern.
     * Bean factory methods should **only return a new instance**; side effects belong in lifecycle callbacks.
     *
     * Moved into [PostConstruct] of a dedicated bridge bean to ensure:
     * - The side effect is decoupled from bean construction
     * - The side effect is not retriggered when the bean is destroyed/recreated
     * - Ordering is explicit: it happens after LocalValidatorFactoryBean is fully initialized
     */
    @Bean
    open fun validationContextBridge(validator: LocalValidatorFactoryBean): ValidationContextBridge =
        ValidationContextBridge(validator)

    /** Bridge bean. Holds a reference to the validator and writes it to [ValidationContext] in PostConstruct. */
    class ValidationContextBridge(private val validator: LocalValidatorFactoryBean) {
        @PostConstruct
        fun bridgeToValidationContext() {
            ValidationContext.validator = validator
        }
    }

    override fun getComponentName() = "kudos-bean-validator"

}