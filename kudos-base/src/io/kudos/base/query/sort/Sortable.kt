package io.kudos.base.query.sort

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

/**
 * 标记数据库表 PO（实体）上允许参与列表查询排序的属性。
 *
 * 客户端请求的排序属性名须与该成员在 PO 上的 **Kotlin 属性名** 一致；否则 DAO 会忽略该排序并打 WARN。
 * 与请求体中的 VO/PO 返回类型无关，仅看当前 DAO 绑定的表实体类型。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Sortable

/**
 * 收集 **表实体 / PO** 类型上带有 [Sortable] 的排序属性名集合（即各成员的 Kotlin 属性名）。
 *
 * 包含：主构造参数（data class）、以及 [memberProperties]（含接口 PO 继承链上的属性）。
 */
fun sortablePropertyNamesForEntity(entityClass: KClass<*>): Set<String> {
    val names = LinkedHashSet<String>()
    entityClass.primaryConstructor?.parameters?.forEach { param ->
        val paramName = param.name ?: return@forEach
        if (param.findAnnotation<Sortable>() != null) {
            names += paramName
        }
    }
    entityClass.memberProperties.forEach { prop ->
        if (prop.findSortable() != null) {
            names += prop.name
        }
    }
    return names
}

private fun KProperty<*>.findSortable(): Sortable? =
    getter.findAnnotation<Sortable>()
        ?: findAnnotation<Sortable>()
        ?: javaField?.getAnnotation(Sortable::class.java)
