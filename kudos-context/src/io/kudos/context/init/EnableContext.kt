package io.kudos.context.init

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import java.lang.annotation.Inherited


@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@ImportAutoConfiguration(ContextAutoConfiguration::class)
annotation class EnableContext