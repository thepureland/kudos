package io.kudos.context.validation

import io.kudos.base.bean.validation.support.ValidationContext
import org.hibernate.validator.HibernateValidator
import org.springframework.beans.factory.annotation.Qualifier
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
open class ValidatorConfiguration {

    @Bean
    @Qualifier("mvcValidator")
    @Primary
    open fun defaultValidator(): LocalValidatorFactoryBean {
        val validator = CustomConstraintValidatorFactory()
        validator.setProviderClass(HibernateValidator::class.java)
        ValidationContext.validator = validator
        return validator
    }

    @Bean
    @Primary
    open fun methodValidationPostProcessor(validator: LocalValidatorFactoryBean): MethodValidationPostProcessor? {
        val processor = MethodValidationPostProcessor()
        processor.setValidator(validator)
        return processor
    }

}