package io.kudos.context.init

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import java.lang.annotation.Inherited
import kotlin.reflect.KClass


/**
 * Kudos赋能注解
 *
 * 将该注解添加到应用的启动类上,将使应用获得Kudos的某些能力或服务,具体哪些能力或服务,取决于应用依赖Kudos的情况.
 *
 * @author K
 * @since 1.0.0
 */
@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@ImportAutoConfiguration(value = [ComponentInitializerSelector::class])
@EnableAutoConfiguration
//@EnableAutoConfiguration(exclude = [ServletWebServerFactoryAutoConfiguration::class])
annotation class EnableKudos(

    /**
     * 要排除的初始化器类
     *
     * @author K
     * @since 1.0.0
     */
    val exclusions: Array<KClass<out IComponentInitializer>> = []

)
