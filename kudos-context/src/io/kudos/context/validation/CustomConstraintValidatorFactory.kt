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

    private lateinit var applicationContext: ApplicationContext

    override fun getClockProvider(): ClockProvider {
        return ClockProvider {
            Clock.systemDefaultZone()
        }
    }

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

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        super.setApplicationContext(applicationContext)
        this.applicationContext = applicationContext
    }

}