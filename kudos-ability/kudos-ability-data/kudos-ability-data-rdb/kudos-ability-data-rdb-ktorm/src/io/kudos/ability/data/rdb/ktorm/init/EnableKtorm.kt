package io.kudos.ability.data.rdb.ktorm.init

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import java.lang.annotation.Inherited


@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@EnableAutoConfiguration
@ImportAutoConfiguration(KtormAutoConfiguration::class)
annotation class EnableKtorm