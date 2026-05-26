package io.kudos.context.init

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import java.lang.annotation.Inherited
import kotlin.reflect.KClass


/**
 * Kudos enablement annotation.
 *
 * Adding this annotation to the application's bootstrap class lets the application acquire certain Kudos capabilities
 * or services; exactly which capabilities or services are enabled depends on how the application depends on Kudos.
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
     * Initializer classes to exclude.
     *
     * @author K
     * @since 1.0.0
     */
    val exclusions: Array<KClass<out IComponentInitializer>> = []

)
