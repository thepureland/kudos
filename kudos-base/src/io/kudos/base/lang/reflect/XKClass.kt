package io.kudos.base.lang.reflect

import java.net.URLDecoder
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

/**
 * kotlin.KClass extension functions.
 *
 * @author K
 * @since 1.0.0
 */


/**
 * Return the no-arg constructor, or null if it does not exist.
 *
 * @param T the current type
 * @return the no-arg constructor
 * @author K
 * @since 1.0.0
 */
fun <T : Any> KClass<T>.getEmptyConstructor(): KFunction<T>? = constructors.firstOrNull { it.parameters.isEmpty() }

/**
 * Instantiate the class.
 *
 * @param T the current type
 * @param args varargs of constructor arguments
 * @return the class instance
 * @author K
 * @since 1.0.0
 */
fun <T : Any> KClass<T>.newInstance(vararg args: Any?): T {
    require(!isAbstract) { "Abstract class $simpleName cannot be instantiated" }
    require(!isCompanion) { "Companion object $simpleName cannot be instantiated" }
    // Find a constructor whose parameter count and types match
    val ctor = constructors.firstOrNull { ctor ->
        ctor.parameters.size == args.size &&
            ctor.parameters.zip(args.toList()).all { (param, arg) ->
                when (arg) {
                    null -> param.type.isMarkedNullable
                    else -> arg::class.isSubclassOf(param.type.jvmErasure)
                }
            }
    } ?: throw IllegalArgumentException(
        "Cannot instantiate $simpleName: no constructor found matching parameter types ${args.map { it?.let { a -> a::class.simpleName } ?: "null" }}"
    )
    return ctor.call(*args)
}

/**
 * Whether the current class is an enum.
 *
 * @return true if the current class is an enum, false otherwise
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.isEnum(): Boolean = java.isEnum

/**
 * Whether the current class is an interface.
 *
 * @return true if the current class is an interface, false otherwise
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.isInterface(): Boolean = isAbstract && constructors.isEmpty()

/**
 * Whether the current class is abstract.
 *
 * @return true if the current class is abstract, false otherwise
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.isAbstractClass(): Boolean = isAbstract && !java.isInterface

/**
 * Whether the current class is an annotation.
 *
 * @return true if the current class is an annotation, false otherwise
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.isAnnotation(): Boolean = java.isAnnotation

/**
 * Whether the specified annotation class is present on this class.
 * Note: ineffective for annotations such as SinceKotlin!
 *
 * @param annotationClass the annotation class
 * @return true if the specified annotation class is present on this class, false otherwise
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.isAnnotationPresent(annotationClass: KClass<out Annotation>): Boolean =
    annotations.any { it.annotationClass == annotationClass }


/**
 * Return the property object for the given property name.
 *
 * @param T the current type
 * @param propertyName the property name
 * @return the property object
 * @throws NoSuchElementException when it does not exist
 * @author K
 * @since 1.0.0
 */
fun <T : Any> KClass<T>.getMemberProperty(propertyName: String): KProperty1<T, Any?> =
    memberProperties.first { it.name == propertyName }

/**
 * Return the property value.
 *
 * @param target the target object
 * @param propertyName the property name
 * @return the property value
 * @throws NoSuchElementException when it does not exist
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.getMemberPropertyValue(target: Any, propertyName: String): Any? =
    getMemberProperty(propertyName).call(target)

/**
 * Return the member function object with the given name and parameters.
 *
 * @param functionName the member function name
 * @param parameters varargs of function parameters
 * @return the member function object
 * @throws NoSuchElementException when it does not exist
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.getMemberFunction(functionName: String, vararg parameters: KParameter): KFunction<*> {
    val functions = memberFunctions.filter { it.name == functionName }
    return when (functions.size) {
        0 -> throw NoSuchElementException("Method named [${functionName}] not found in class [${this}]!")
        1 -> functions.first()
        else -> functions.firstOrNull { fn ->
            fn.parameters.indices.all { i -> fn.parameters[i].type == parameters[i].type }
        } ?: throw NoSuchElementException("Method named [${functionName}] with matching parameter types not found in class [${this}]!")
    }
}

/**
 * Return the direct superclass of the current class.
 *
 * @return the direct superclass, or null if the current class is Any
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.getSuperClass(): KClass<*>? {
    if (this == Any::class) return null
    return superclasses.first { it.constructors.isNotEmpty() }
}


/**
 * Return the direct super-interfaces of the current class.
 *
 * @return the list of direct super-interfaces
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.getSuperInterfaces(): List<KClass<*>> = java.interfaces.map { it.kotlin }

/**
 * Return all interfaces implemented by the current class.
 *
 * @return all interfaces
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.getAllInterfaces(): List<KClass<*>> = allSuperclasses.filter { it.java.isInterface }

/**
 * Match the first Type that represents the current class.
 *
 * @param types the collection of types to search
 * @return the first Type representing the current class
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.firstMatchTypeOf(types: Collection<KType>): KType = types.first { it.classifier == this }

/**
 * Return the set of classes in the type hierarchy (upwards) of the specified class that carry the given annotation.
 * Note: ineffective for annotations such as SinceKotlin!
 *
 * @param annoClass the annotation class
 * @return the set of matching classes
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.getClassUpThatPresentAnnotation(annoClass: KClass<out Annotation>): Set<KClass<*>>
    = sequenceOf(this)
    // Include all direct and indirect superclasses/super-interfaces
    .plus(allSuperclasses.asSequence())
    // Keep only classes carrying the specified annotation
    .filter { k -> k.annotations.any { it.annotationClass == annoClass } }
    .toSet()


/**
 * Get the physical location of the class on disk.
 *
 * @return the absolute path to the class file
 * @since 1.0.0
 * @author K
 * @since 1.0.0
 */
fun KClass<*>.getLocationOnDisk(): String =
    URLDecoder.decode(java.protectionDomain.codeSource.location.path, "UTF-8")
