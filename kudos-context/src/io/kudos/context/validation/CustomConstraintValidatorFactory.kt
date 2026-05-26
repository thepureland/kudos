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
 * Custom constraint validator factory used to associate validators with custom constraint annotations.
 *
 * @author K
 * @since 1.0.0
 */
open class CustomConstraintValidatorFactory: LocalValidatorFactoryBean(), ApplicationContextAware {

    /** Spring context, injected via [setApplicationContext]; used to resolve all [IConstraintValidatorProviderBean] instances. */
    private lateinit var applicationContext: ApplicationContext

    /**
     * Overrides the default [ClockProvider] so that time-related constraints such as `@Future` / `@Past`
     * use the system-default-zone clock.
     *
     * @return always a [Clock] using the system default time zone
     * @author K
     * @since 1.0.0
     */
    override fun getClockProvider(): ClockProvider = ClockProvider { Clock.systemDefaultZone() }

    /**
     * Post-processes the Hibernate Validator configuration: collects all
     * [IConstraintValidatorProviderBean] instances from the container and registers their declared
     * "constraint annotation -> validator" mappings into
     * [HibernateValidatorConfiguration.createConstraintMapping].
     *
     * `includeExistingValidators(false)` is used to fully replace the default implementations: validators
     * explicitly declared by the business side via a provider win over the jakarta built-in implementations
     * for the same constraint, making it easy to customize built-in annotations (for example to change the
     * error semantics of @Pattern).
     *
     * @param configuration the configurable items injected by HV
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
     * The context is injected by Spring via [ApplicationContextAware].
     * First calls the parent logic (so LocalValidatorFactoryBean can also use the context), then caches
     * a reference here for [postProcessConfiguration].
     *
     * @param applicationContext the Spring application context
     * @author K
     * @since 1.0.0
     */
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        super.setApplicationContext(applicationContext)
        this.applicationContext = applicationContext
    }

}