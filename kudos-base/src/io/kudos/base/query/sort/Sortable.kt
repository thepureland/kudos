package io.kudos.base.query.sort

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

/**
 * Marks a property on a database table PO (entity) that is allowed to participate in list query sorting.
 *
 * The sort property name in client requests must match the member's **Kotlin property name** on the PO;
 * otherwise the DAO ignores that sort and emits a WARN. Independent of the VO/PO return type in the request
 * body — only the table entity type currently bound to the DAO is considered.
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
 * Collects the set of sortable property names (Kotlin property names of each member) annotated with [Sortable]
 * on the **table entity / PO** type.
 *
 * Includes: primary constructor parameters (data class) and [memberProperties] (including properties from the
 * interface PO inheritance chain).
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

/**
 * Looks up [Sortable] in the three possible locations on a KProperty: getter -> property itself -> java field.
 *
 * Kotlin annotation sites are position-sensitive (FIELD vs PROPERTY vs GETTER are independent), and the
 * declarer may place the annotation in any of them — try all three and take the first non-null. Semantically
 * all three are treated as "this property is sortable".
 *
 * @return the annotation instance if found; null otherwise
 * @author K
 * @since 1.0.0
 */
private fun KProperty<*>.findSortable(): Sortable? =
    getter.findAnnotation<Sortable>()
        ?: findAnnotation<Sortable>()
        ?: javaField?.getAnnotation(Sortable::class.java)
