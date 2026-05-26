package io.kudos.base.bean.validation.support

import io.kudos.base.bean.BeanKit
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Validator
import jakarta.validation.metadata.ConstraintDescriptor
import jakarta.validation.metadata.PropertyDescriptor
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorInitializationContext
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Bean validation context.
 *
 * @author K
 * @since 1.0.0
 */
object ValidationContext {


    /** Cached HV initializationContext. */
    private var hvInitCtx: HibernateConstraintValidatorInitializationContext? = null
    /** Jakarta standard constraint annotation package prefix, used to filter out non-business annotations. */
    private const val jakartaAnnotationPrefix = "jakarta"
    /** Hibernate Validator built-in constraint annotation package prefix, used to filter out non-business annotations. */
    private const val hibernateAnnotationPrefix = "org.hibernate"
    /** Cache of reflectively-resolved `getValidatorFactoryScopedContext` methods. */
    private val scopedContextMethodCache = ConcurrentHashMap<Class<*>, Method>()
    /** Cache of reflectively-resolved `validatorFactoryScopedContext` fields (falls back to the field if the method lookup fails). */
    private val scopedContextFieldCache = ConcurrentHashMap<Class<*>, Field>()
    /** Cache of reflectively-resolved `getConstraintValidatorInitializationContext` methods. */
    private val initCtxMethodCache = ConcurrentHashMap<Class<*>, Method>()
    /** Cache of whether an annotation class is a "business custom constraint": package name belongs to neither jakarta nor org.hibernate. */
    private val businessConstraintAnnotationCache = ConcurrentHashMap<Class<out Annotation>, Boolean>()

    /**
     * Maps the runtime class of ConstraintValidatorContext (HV's ConstraintValidatorContextImpl, etc.) to
     * a function that can extract a ConstraintDescriptor from it. Using reflection avoids hard binding to an HV internal class
     * (previously `as? ConstraintValidatorContextImpl`, which broke compilation when HV renamed it).
     */
    private val descriptorAccessorCache = ConcurrentHashMap<Class<*>, (Any) -> ConstraintDescriptor<*>?>()

    /**
     * Inject the built [jakarta.validation.ValidatorFactory] and immediately extract and cache the Hibernate Validator
     * initialization context from it.
     * Call this once during application startup.
     *
     * @param factory the constructed ValidatorFactory
     * @author K
     * @since 1.0.0
     */
    fun setFactory(factory: jakarta.validation.ValidatorFactory) {
        hvInitCtx = extractHvInitCtx(factory)
    }

    /**
     * Returns the cached [HibernateConstraintValidatorInitializationContext].
     *
     * @return the HV initialization context
     * @throws IllegalStateException if [setFactory] has not been called yet to inject the ValidatorFactory
     * @author K
     * @since 1.0.0
     */
    fun getHvInitCtx(): HibernateConstraintValidatorInitializationContext =
        hvInitCtx ?: error("HibernateConstraintValidatorInitializationContext is not initialized yet: make sure ValidationKit.getValidator() is called first to build the ValidatorFactory")

    /**
     * Extracts the HibernateConstraintValidatorInitializationContext from a Hibernate Validator ValidatorFactory.
     *
     * Why is this needed?
     * - In custom composed constraints (@Constraints), you "manually" create and call built-in ConstraintValidator instances (such as PatternValidator backing @Pattern).
     * - Starting from HV 9.1, some built-in validators implement HibernateConstraintValidator,
     *   and expect to be initialized via HibernateConstraintValidator#initialize(ConstraintDescriptor, HibernateConstraintValidatorInitializationContext)
     *   (e.g. compiling regex Pattern, building internal state).
     * - But when calling them manually, the Hibernate Validator engine's initialization flow is bypassed, so the initCtx must be obtained
     *   and used to initialize again; otherwise internal fields may remain uninitialized and trigger an NPE (the case you hit was PatternValidator.pattern being null).
     *
     * Note:
     * - This implementation relies on HV internal API (org.hibernate.validator.internal.*); minor version bumps may rename methods/fields.
     * - It is therefore written as "multi-strategy attempts + fallback" so it keeps working across small version changes.
     *
     * @author AI: ChatGPT
     * @since 1.0.0
     */
    private fun extractHvInitCtx(factory: jakarta.validation.ValidatorFactory): HibernateConstraintValidatorInitializationContext {
        // 1) Try the method first: factory.getValidatorFactoryScopedContext()
        val scopedContext = runCatching {
            val method = scopedContextMethodCache.getOrPut(factory.javaClass) {
                factory.javaClass.getDeclaredMethod("getValidatorFactoryScopedContext").apply { isAccessible = true }
            }
            method.invoke(factory)
        }.getOrNull()
            ?: runCatching {
                // 2) Fall back to the field: factory.validatorFactoryScopedContext
                val field = scopedContextFieldCache.getOrPut(factory.javaClass) {
                    factory.javaClass.getDeclaredField("validatorFactoryScopedContext").apply { isAccessible = true }
                }
                field.get(factory)
            }.getOrNull()
            ?: error("Unable to obtain ValidatorFactoryScopedContext from ${factory.javaClass.name} (HV version/implementation may have changed)")

        // 3) scopedContext.getConstraintValidatorInitializationContext()
        val initCtx = runCatching {
            val method = initCtxMethodCache.getOrPut(scopedContext.javaClass) {
                scopedContext.javaClass.getDeclaredMethod("getConstraintValidatorInitializationContext")
                    .apply { isAccessible = true }
            }
            method.invoke(scopedContext)
        }.getOrNull()
            ?: error("Unable to obtain ConstraintValidatorInitializationContext from ${scopedContext.javaClass.name} (HV version/implementation may have changed)")

        return initCtx as? HibernateConstraintValidatorInitializationContext
            ?: error("ConstraintValidatorInitializationContext type mismatch: ${initCtx.javaClass.name}")
    }

    /** Used to pass the Bean to ConstraintValidator, since Hibernate validation's ConstraintValidatorContext cannot access the Bean. */
    private val beanMapThreadLocal = ThreadLocal.withInitial { mutableMapOf<Int, Any>() } // Map<ConstraintDescriptor hashcode, Bean instance>

    /** Whether fail-fast mode is enabled. */
    private val failFastThreadLocal = InheritableThreadLocal<Boolean>()

    /** The validator. */
    var validator: Validator? = null

    /**
     * Stores the Bean associated with the ConstraintDescriptor's hashcode.
     *
     * @param validator the validator
     * @param bean the Bean to validate
     * @author K
     * @since 1.0.0
     */
    fun set(validator: Validator, bean: Any) {
        set(validator, bean, null, beanMapThreadLocal.get())
    }

    /**
     * Recursively traverses all constrained properties of the bean and associates each non-Jakarta/HV constraint
     * [ConstraintDescriptor] hashcode with the bean itself, storing it in [beanStore], so that custom constraint
     * validators can later retrieve the bean.
     *
     * Nested `@Valid` properties are processed recursively; List elements are joined to the path using `[i]`.
     *
     * @param validator the [Validator] currently in use
     * @param bean the bean to store in the context
     * @param parentPath the parent path (used for display of nested properties; not currently exposed)
     * @param beanStore thread-local storage
     * @author K
     * @since 1.0.0
     */
    private fun set(
        validator: Validator,
        bean: Any,
        parentPath: String?,
        beanStore: MutableMap<Int, Any>
    ) {
        // Obtain the bean's descriptor
        val beanDescriptor = validator.getConstraintsForClass(bean.javaClass)

        // Iterate over every constrained property of the bean
        beanDescriptor.constrainedProperties.forEach { descriptor: PropertyDescriptor ->
            val propertyName = descriptor.propertyName
            // When joining paths, ensure there is no leading "." when parentPath is empty
            val fullPath =
                if (parentPath.isNullOrEmpty()) propertyName else "$parentPath.$propertyName"

            // Check each constraint on the property
            descriptor.constraintDescriptors.forEach { des: ConstraintDescriptor<*> ->
                val annotationClass = des.annotation.annotationClass.java
                val isBusinessConstraint = businessConstraintAnnotationCache.getOrPut(annotationClass) {
                    val annoClassName = annotationClass.name
                    !annoClassName.startsWith(jakartaAnnotationPrefix) &&
                        !annoClassName.startsWith(hibernateAnnotationPrefix)
                }
                // Filter out Jakarta and Hibernate annotations
                if (isBusinessConstraint) {
                    beanStore[des.hashCode()] = bean
                }
            }

            // Determine whether this is a nested object, i.e. whether the property has other constrained properties (nested validation)
            if (descriptor.isCascaded) {
                when (val nestedBean = BeanKit.getProperty(bean, propertyName)) {
                    null -> {}
                    is MutableList<*> -> nestedBean.forEachIndexed { i, el ->
                        // For each list element, recursively validate and append the index to the path
                        el?.let { set(validator, it, "$fullPath[$i]", beanStore) }
                    }
                    else -> set(validator, nestedBean, fullPath, beanStore)
                }
            }
        }
    }

    /**
     * Returns the Bean associated with the ConstraintDescriptor's hashcode and removes it from the context.
     *
     * @param constraintValidatorContext the constraint validator context
     * @return the Bean being validated
     * @author K
     * @since 1.0.0
     */
    fun get(constraintValidatorContext: ConstraintValidatorContext): Any? {
        val descriptor = extractConstraintDescriptor(constraintValidatorContext) ?: return null
        return beanMapThreadLocal.get().remove(descriptor.hashCode())
    }

    /**
     * Reflectively extracts the [ConstraintDescriptor] from any runtime implementation of [ConstraintValidatorContext]
     * without hard-binding to HV's concrete implementation class name (previously `as? ConstraintValidatorContextImpl`,
     * which would fail to compile when HV repackaged/renamed it). It first looks for a `getConstraintDescriptor()` method,
     * then falls back to a `constraintDescriptor` field.
     *
     * The reflection accessor for each concrete ctx class is built only once and cached in [descriptorAccessorCache].
     */
    private fun extractConstraintDescriptor(ctx: ConstraintValidatorContext): ConstraintDescriptor<*>? {
        val accessor = descriptorAccessorCache.getOrPut(ctx.javaClass) { buildDescriptorAccessor(ctx.javaClass) }
        return accessor(ctx)
    }

    /**
     * Builds a descriptor-reader closure for the given ConstraintValidatorContext runtime class.
     * Prefer the `getConstraintDescriptor()` method; if the method is missing, fall back to the `constraintDescriptor` field;
     * as a last resort, return a constant null closure (suitable for mocks or non-HV implementations).
     *
     * @param clazz the concrete runtime class of ConstraintValidatorContext
     * @return a closure that maps a ctx instance to its [ConstraintDescriptor]
     * @author K
     * @since 1.0.0
     */
    private fun buildDescriptorAccessor(clazz: Class<*>): (Any) -> ConstraintDescriptor<*>? {
        findInHierarchy(clazz) { it.getDeclaredMethod("getConstraintDescriptor") }?.let { method ->
            method.isAccessible = true
            return { runCatching { method.invoke(it) as? ConstraintDescriptor<*> }.getOrNull() }
        }
        findInHierarchy(clazz) { it.getDeclaredField("constraintDescriptor") }?.let { field ->
            field.isAccessible = true
            return { runCatching { field.get(it) as? ConstraintDescriptor<*> }.getOrNull() }
        }
        // Reaching here usually means a mock or non-HV implementation; return null so get() falls back
        return { _ -> null }
    }

    /**
     * Walks up the inheritance chain and applies [finder] to each level.
     * Stops once any level returns non-null; a thrown exception from finder is treated as a miss for that level.
     *
     * @param R the lookup target type ([Method] or [Field])
     * @param clazz the starting class
     * @param finder the closure that performs the lookup on a single class
     * @return the first hit; null when none of the levels match
     * @author K
     * @since 1.0.0
     */
    private inline fun <R : Any> findInHierarchy(clazz: Class<*>, finder: (Class<*>) -> R): R? {
        var current: Class<*>? = clazz
        while (current != null) {
            runCatching { return finder(current) }
            current = current.superclass
        }
        return null
    }

    /**
     * Clears the currently cached Bean mappings to avoid leftovers across validation calls.
     */
    fun clearBeans() {
        beanMapThreadLocal.remove()
    }

    /**
     * Sets fail-fast mode.
     *
     * @param failFast true for fail-fast mode; false otherwise
     * @author K
     * @since 1.0.0
     */
    fun setFailFast(failFast: Boolean) = failFastThreadLocal.set(failFast)

    /**
     * Returns the fail-fast mode flag.
     *
     * @return true for fail-fast mode; false otherwise
     * @author K
     * @since 1.0.0
     */
    fun isFailFast(): Boolean = failFastThreadLocal.get() ?: true

}
