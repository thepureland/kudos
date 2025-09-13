package io.kudos.ability.data.rdb.jdbc.aop

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DsChange(val value: String = "", val readonly: Boolean = false)
