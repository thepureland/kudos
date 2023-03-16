package io.kudos.ability.data.rdb.ktorm.init

import org.soul.context.context.SoulContextBeanDefinitionRegistrar
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited


@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@ImportAutoConfiguration(KtormAutoConfiguration::class)
annotation class EnableKtorm