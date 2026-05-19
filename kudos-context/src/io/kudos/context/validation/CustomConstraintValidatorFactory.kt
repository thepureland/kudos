package io.kudos.context.validation

import jakarta.validation.ClockProvider
import jakarta.validation.Configuration
import jakarta.validation.ConstraintValidator
import org.hibernate.validator.HibernateValidatorConfiguration
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.time.Clock


/**
 * 自定义约束的验证器工厂，用于为自定义的约束注解指定验证器
 *
 * @author K
 * @since 1.0.0
 */
open class CustomConstraintValidatorFactory: LocalValidatorFactoryBean(), ApplicationContextAware {

    /** Spring 上下文，由 [setApplicationContext] 注入；用于解析所有 [IConstraintValidatorProviderBean] */
    private lateinit var applicationContext: ApplicationContext

    /**
     * 覆盖默认 [ClockProvider]，让 `@Future` / `@Past` 等时间相关约束使用系统默认时区时钟。
     *
     * @return 始终返回系统默认时区的 [Clock]
     * @author K
     * @since 1.0.0
     */
    override fun getClockProvider(): ClockProvider = ClockProvider { Clock.systemDefaultZone() }

    /**
     * Hibernate Validator 配置后处理：收集容器里所有 [IConstraintValidatorProviderBean]，
     * 把它们声明的「约束注解 → 校验器」关系注册到 [HibernateValidatorConfiguration.createConstraintMapping]。
     *
     * `includeExistingValidators(false)` 是为了完全替换默认实现——业务侧通过 provider 显式声明的
     * 校验器会胜过 jakarta 自带的同名约束实现，方便对内置注解做定制（如改 @Pattern 报错语义等）。
     *
     * @param configuration HV 注入的可配置项
     * @author K
     * @since 1.0.0
     */
    override fun postProcessConfiguration(configuration: Configuration<*>) {
        val hibernateConfiguration = configuration as HibernateValidatorConfiguration
        val constraintMapping = hibernateConfiguration.createConstraintMapping()
        val beans = applicationContext.getBeansOfType<IConstraintValidatorProviderBean>().values
        beans.forEach { provider ->
            val validators = provider.provide<Annotation, ConstraintValidator<Annotation, *>>()
            validators.forEach { (constraint, validator) ->
                constraintMapping
                    .constraintDefinition(constraint.java)
                    .validatedBy(validator.java)
                    .includeExistingValidators(false)
            }
        }
        hibernateConfiguration.addMapping(constraintMapping)
    }

    /**
     * 由 Spring 通过 [ApplicationContextAware] 注入上下文。
     * 先调父类逻辑（让 LocalValidatorFactoryBean 也能用到 context），再缓存一份到本类供 [postProcessConfiguration] 使用。
     *
     * @param applicationContext Spring 上下文
     * @author K
     * @since 1.0.0
     */
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        super.setApplicationContext(applicationContext)
        this.applicationContext = applicationContext
    }

}