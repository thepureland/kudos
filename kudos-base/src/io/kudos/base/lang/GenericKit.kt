package io.kudos.base.lang

import io.kudos.base.lang.reflect.firstMatchTypeOf
import io.kudos.base.lang.reflect.getSuperClass
import io.kudos.base.lang.reflect.getSuperInterfaces
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

/**
 * Generic utility.
 *
 * @author K
 * @since 1.0.0
 */
object GenericKit {


    /**
     * Get the actual type of the generic parameter of the direct superclass of the specified class; if there is no non-Any superclass, take the first implemented interface.
     *
     * @param clazz the class whose generic parameter's actual type is to be obtained; this class must extend a generic superclass or implement a generic interface
     * @param index the index of the generic parameter, starting from 0.
     * @return the actual type of the generic parameter. Returns Nothing::class if generics are unsupported; returns Any::class when the generic parameter is "*"; returns null when the index is out of bounds.
     * @author K
     * @since 1.0.0
     */
    fun getSuperClassGenricClass(clazz: KClass<*>, index: Int = 0): KClass<*> {
        if (clazz == Any::class) {
            return Nothing::class
        }

        val directSuperClass = requireNotNull(clazz.getSuperClass()) { "Class [$clazz] has no usable superclass" }
        var directSuperType = directSuperClass.firstMatchTypeOf(clazz.supertypes)
        if (directSuperType == Any::class.starProjectedType) {
            // No non-Any superclass; take the first implemented interface
            val genericInterfaces = clazz.getSuperInterfaces()
            if (genericInterfaces.isNotEmpty()) {
                directSuperType = genericInterfaces[0].firstMatchTypeOf(clazz.supertypes)
            }
        }

        val args = directSuperType.arguments
        if(args.isEmpty()) {
            // Parameterization may be done on the parent class
            return getSuperClassGenricClass(directSuperClass, index) // walk up to the parent
        }

        require(index in args.indices) {
            "The supplied index ${if (index < 0) "cannot be negative" else "exceeds the total number of parameters"}"
        }
        val type = args[index].type
        return type?.jvmErasure ?: Any::class // returns Any::class when the generic parameter is *
    }


    /**
     * Get the actual types of all generic parameters of the index-th input parameter type of the callable.
     *
     * @param callable the callable, e.g. a function
     * @param index the position of the input parameter. The 0th parameter is the object on which the function is invoked (passed implicitly), so visible function parameters start from 1.
     * @return the list of actual types of the generic parameters. If generics are unsupported, the element type is Nothing::class; if the generic parameter is "*", the element type is Any::class; if the index is out of bounds, the element is null.
     * @author K
     * @since 1.0.0
     */
    fun getParameterTypeGenericClass(callable: KCallable<*>, index: Int = 1): List<KClass<*>> {
        require(index in callable.parameters.indices) {
            "The supplied index ${if (index < 0) "cannot be negative" else "exceeds the total number of parameters"}"
        }
        val args = callable.parameters[index].type.arguments
        if (args.isEmpty()) return listOf(Nothing::class)
        return args.map { it.type?.jvmErasure ?: Any::class }
    }

    /**
     * Get the actual type of the generic parameter of the callable's return type.
     *
     * @param callable the callable, e.g. a property or function
     * @param index the index of the generic parameter, starting from 0.
     * @return the actual type of the generic parameter. Returns Nothing::class if generics are unsupported; returns Any::class when the generic parameter is "*"; returns null when the index is out of bounds.
     * @author K
     * @since 1.0.0
     */
    fun getReturnTypeGenericClass(callable: KCallable<*>, index: Int = 0): KClass<*> {
        val args = callable.returnType.arguments
        if (args.isEmpty()) {
            return Nothing::class
        }
        require(index in args.indices) {
            "The supplied index ${if (index < 0) "cannot be negative" else "exceeds the total number of generic parameters"}"
        }
        val type = args[index].type
        return type?.jvmErasure ?: Any::class // returns Any::class when the generic parameter is *
    }

}
