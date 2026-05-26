package io.kudos.base.bean

import io.kudos.base.lang.SerializationKit
import io.kudos.base.lang.reflect.getEmptyConstructor
import io.kudos.base.model.contract.entity.IIdEntity
import org.apache.commons.beanutils.BeanUtils
import org.apache.commons.beanutils.ConvertUtils
import org.apache.commons.beanutils.PropertyUtils
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.io.Serializable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Bean operation utility class.
 *
 * @author K
 * @since 1.0.0
 */
object BeanKit {

    /**
     * Deeply clone the specified bean.
     * This method is many times slower than overriding the clone method on every object in the object graph directly. However, for complex object graphs, or for objects that do not support deep cloning, this provides an alternative implementation. Of course, all objects must implement the `Serializable` interface.
     *
     * @param T bean type
     * @param bean the bean to be cloned
     * @return the cloned bean
     * @throws org.apache.commons.lang3.SerializationException (runtime) if serialization fails
     * @see SerializationKit.clone
     * @author K
     * @since 1.0.0
     */
    fun <T : Serializable> deepClone(bean: T): T = SerializationKit.clone(bean)

    /**
     * Copy the properties of the source object to the corresponding properties of the specified target class object, according to the field mapping.
     *
     * @param T target type
     * @param destClass target class
     * @param srcObj source object
     * @param propertyMap field mapping Map(source object property name, target object property name); if null or empty, will try to copy all source object properties to the corresponding properties of the target object (if they exist)
     * @return the target class object
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> copyProperties(destClass: KClass<T>, srcObj: Any, propertyMap: Map<String, String>? = null): T {
        val constructor = requireNotNull(destClass.getEmptyConstructor()) {
            "Class ${destClass.qualifiedName} must provide a no-arg constructor in order to perform property copying."
        }
        val destObj = constructor.call()
        copyProperties(srcObj, destObj, propertyMap)
        return destObj
    }

    /**
     * Copy the properties of the source object to the corresponding properties of the specified target object, according to the field mapping.
     *
     * @param T target type
     * @param srcObj source object
     * @param destObj target object
     * @param propertyMap field mapping Map(source object property name, target object property name); if null or empty, will try to copy all source object properties to the corresponding properties of the target object (if they exist)
     * @return the target class object
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> copyProperties(srcObj: Any, destObj: T, propertyMap: Map<String, String>? = null): T {
        val map = if (propertyMap.isNullOrEmpty()) { // Will copy all properties of the source object
            val desProps = destObj::class.memberProperties.map { it.name }.toSet()
            srcObj::class.memberProperties
                .filter { it.name in desProps }
                .associate { it.name to it.name }
        } else propertyMap

        map.forEach { (srcPropertyName, destPropertyName) ->
            if (srcPropertyName.isBlank() || destPropertyName.isBlank()) {
                return@forEach
            }
            val result = getProperty(srcObj, srcPropertyName)
            setProperty(destObj, destPropertyName, result)
        }
        return destObj
    }

    /**
     * Copy all properties except the primary key.
     *
     * @param T entity object type
     * @param src source object
     * @param dest target object
     * @return the target class object
     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
     * @throws IllegalAccessException if the requested method cannot be accessed via reflection
     * @author K
     * @since 1.0.0
     */
    fun <T> copyPropertiesExcludeId(src: IIdEntity<T>, dest: IIdEntity<T>): IIdEntity<T> {
        val id = dest.id
        copyProperties(src, dest, null)
        setPropertyByField(dest, "id", id)
        return dest
    }

    /**
     * Copy the object, excluding the specified properties (does not support nested/indexed/mapped/composite).
     *
     * @param T target type
     * @param source source object
     * @param target target object
     * @param excludeProperties vararg array of properties not to copy
     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
     * @throws IllegalAccessException if the requested method cannot be accessed via reflection
     * @throws NoSuchMethodException if the specified accessible method cannot be found
     * @throws java.beans.IntrospectionException introspection exception
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> copyPropertiesExclude(source: Any, target: T, vararg excludeProperties: String): T {
        val beanInfo = Introspector.getBeanInfo(target.javaClass)
        val targetPds = beanInfo.propertyDescriptors
        for (targetPd in targetPds) {
            if (targetPd.writeMethod != null && targetPd.name !in excludeProperties) {
                val sourcePd: PropertyDescriptor = PropertyUtils.getPropertyDescriptor(source, targetPd.name)
                sourcePd.readMethod?.run {
                    if (!Modifier.isPublic(declaringClass.modifiers)) {
                        isAccessible = true
                    }
                    val value = invoke(source)
                    val writeMethod = targetPd.writeMethod
                    if (!Modifier.isPublic(writeMethod.declaringClass.modifiers)) {
                        writeMethod.isAccessible = true
                    }
                    writeMethod.invoke(target, value)
                }
            }
        }
        return target
    }

    /**
     * Reset the values of all non-id properties.
     *
     * @param T entity type
     * @param entity target entity bean
     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
     * @throws IllegalAccessException if the requested method cannot be accessed via reflection
     * @throws InstantiationException instantiation exception
     * @author K
     * @since 1.0.0
     */
    fun <T> resetPropertiesExcludeId(entity: IIdEntity<T>) {
        val id = entity.id
        val constructor = requireNotNull(entity::class.getEmptyConstructor()) {
            "Class ${entity::class.qualifiedName} must provide a no-arg constructor in order to reset properties."
        }
        val emptyEntity: IIdEntity<T> = constructor.call()
        copyProperties(emptyEntity, entity, null)
        setPropertyByField(entity, "id", id)
    }

    /**
     * Batch property copy.
     *
     * @param T target type
     * @param targetClass target class
     * @param srcObjs collection of source objects
     * @return List(target class object)
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> batchCopyProperties(targetClass: KClass<T>, srcObjs: Collection<Any>): List<T> =
        srcObjs.map { copyProperties(targetClass, it) }

    //region Wraps org.apache.commons.beanutils.BeanUtils and PropertyUtils
    /**
     * Clone (shallow clone) a bean based on the available property getters and setters, even if the bean itself does not implement the Cloneable interface.
     *
     * @param T bean type
     * @param bean the bean to be cloned
     * @return the cloned bean
     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
     * @throws IllegalAccessException if the requested method cannot be accessed via reflection
     * @throws NoSuchMethodException if the specified accessible method cannot be found
     * @throws InstantiationException instantiation exception
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> shallowClone(bean: T): T {
        val cloned = BeanUtils.cloneBean(bean)
        return bean::class.java.cast(cloned)
    }

//    /**
//     * Copy (shallow clone) all property values of the source bean to the same property values of the target bean; supports type conversion.
//     *
//     * @param orig source bean
//     * @param dest target bean
//     * @param T target type
//     * @return target object
//     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
//     * @throws IllegalAccessException if the requested method cannot be accessed via reflection
//     * @author K
//     * @since 1.0.0
//     */
//    fun <T> copyProperties(orig: Any?, dest: T): T {
//        BeanUtils.copyProperties(orig, dest)
////        org.springframework.beans.BeanUtils.copyProperties(orig, dest)
////        return dest
//        // reserved
//    }

    /**
     * Copy (shallow clone) all property values of the source bean to the same property values of the target bean; does not support type conversion.
     *
     * @param T target bean type
     * @param orig source bean
     * @param dest target bean
     * @return target bean
     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
     * @throws IllegalAccessException if the requested method cannot be accessed via reflection
     * @throws NoSuchMethodException if the specified accessible method cannot be found
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> copyProperties(orig: Any, dest: T): T {
        PropertyUtils.copyProperties(dest, orig)
        return dest
    }

    /**
     * Return all property names and their values of the specified bean.
     *
     * @param bean the bean to extract properties from
     * @return Map(property name, property value)
     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
     * @throws IllegalAccessException if the requested method cannot be accessed via reflection
     * @throws NoSuchMethodException if the specified accessible method cannot be found
     * @author K
     * @since 1.0.0
     */
    fun extract(bean: Any): Map<String, Any?> = PropertyUtils.describe(bean)

    /**
     * Return the value of the specified property.
     *
     * @param bean target bean
     * @param name property name (may be nested/indexed/mapped/composite)
     * @return property value
     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
     * @throws IllegalAccessException if the requested method cannot be accessed via reflection
     * @throws NoSuchMethodException if the specified accessible method cannot be found
     * @author K
     * @since 1.0.0
     */
    fun getProperty(bean: Any, name: String): Any? = PropertyUtils.getProperty(bean, name)

    /**
     * Set property value (shallow clone); supports type conversion.
     *
     * @param T bean type
     * @param bean target bean
     * @param name property name (may be nested/indexed/mapped/composite)
     * @param value property value
     * @return target bean
     * @throws java.lang.reflect.InvocationTargetException if an exception occurs while invoking the target
     * @throws IllegalAccessException
     * @author K
     * @since 1.0.0
     */
    fun <T> setProperty(bean: T, name: String?, value: Any?): T {
        if (name.isNullOrBlank()) return bean
        val pd = PropertyUtils.getPropertyDescriptor(bean, name)
        if (pd?.writeMethod != null) {
            BeanUtils.copyProperty(bean, name, value)
        } else {
            setPropertyByField(bean as Any, name, value)
        }
        return bean
    }

    /**
     * Set a field value directly via reflection; used for read-only properties without a setter (e.g. a Kotlin data class `val`).
     *
     * Field lookup tries two namings: the original property name; and the JavaBean-style `isXxx` -> `xxx` fallback.
     * Searches up the inheritance chain; once found, performs a type conversion via [ConvertUtils] if necessary before assigning.
     *
     * @param bean target object
     * @param name property name
     * @param value value to write; null is written directly
     * @throws NoSuchFieldException when the field cannot be found anywhere in the inheritance chain
     * @author K
     * @since 1.0.0
     */
    private fun setPropertyByField(bean: Any, name: String, value: Any?) {
        val fieldNamesToTry = sequence {
            yield(name)
            // JavaBean boolean is usually exposed as isXxx, with the backing field typically named xxx
            if (name.startsWith("is") && name.length > 2 && Character.isUpperCase(name[2])) {
                yield(name[2].lowercaseChar().toString() + name.substring(3))
            }
        }
        var clazz: Class<*>? = bean.javaClass
        while (clazz != null) {
            val currentClazz: Class<*> = clazz
            val field = fieldNamesToTry.firstNotNullOfOrNull { fieldName ->
                runCatching { currentClazz.getDeclaredField(fieldName) }.getOrNull()
            }
            if (field == null) {
                clazz = clazz.superclass
                continue
            }
            field.setAccessible(true)
            val targetType = field.type
            val toSet = when {
                value == null -> null
                targetType.isAssignableFrom(value.javaClass) -> value
                else -> try {
                    ConvertUtils.convert(value, targetType)
                } catch (_: Exception) {
                    value
                }
            }
            field.set(bean, toSet)
            return
        }
        throw NoSuchFieldException("Property '$name' has no setter and no accessible field in class '${bean.javaClass.name}'.")
    }

    //endregion Wraps org.apache.commons.beanutils.BeanUtils and PropertyUtils

}