package io.kudos.context.kit

import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

/**
 * Spring utility.
 *
 * @author K
 * @since 1.0.0
 */
object SpringKit {

    /**
     * Spring application context. Injected by [io.kudos.context.spring.SpringContextInitializer]
     * during Spring startup.
     *
     * Marked `@Volatile`: writes happen on the main thread (during Spring startup), while reads may come from any business thread.
     * Without volatile, cross-thread reads are not guaranteed to see the write — especially the lazy init of some `object`s in different threads,
     * where the first access can read null even though init has already completed.
     */
    @Volatile
    var applicationContext: ApplicationContext? = null
        get() = field ?: error("Spring applicationContext is not initialized yet!")

    // Use the [applicationContext] getter (which throws when null) to confine `!!` to one place
    private val ctx: ApplicationContext get() = applicationContext!!

    /**
     * Returns the Spring bean with the given name; throws BeansException if it does not exist.
     *
     * @param beanName the bean name
     * @return the Spring bean
     * @author K
     * @since 1.0.0
     */
    fun getBean(beanName: String): Any = ctx.getBean(beanName)

    /**
     * Returns the Spring bean with the given name, or null if it does not exist.
     *
     * @param beanName the bean name
     * @return the Spring bean
     * @author K
     * @since 1.0.0
     */
    fun getBeanOrNull(beanName: String): Any? = if (ctx.containsBean(beanName)) ctx.getBean(beanName) else null

    /**
     * Returns the Spring bean of the given class; throws BeansException if it does not exist.
     *
     * @param T the bean type
     * @param beanClass the bean class
     * @return the Spring bean
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBean(beanClass: KClass<T>): T = ctx.getBean(beanClass.java)

    /**
     * Returns the Spring bean of the given generic type; throws BeansException if it does not exist.
     *
     * @param T the bean type
     * @return the Spring bean
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> getBean(): T = getBean(T::class)

    /**
     * Returns the Spring bean of the given class, or null if it does not exist.
     *
     * @param T the bean type
     * @param beanClass the bean class
     * @return the Spring bean
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBeanOrNull(beanClass: KClass<T>): T? =
        if (ctx.getBeanNamesForType(beanClass.java).isNotEmpty()) ctx.getBean(beanClass.java) else null

    /**
     * Returns the value of the property with the given name.
     *
     * @param propertyName the property name
     * @return the property value
     * @author K
     * @since 1.0.0
     */
    fun getProperty(propertyName: String): String? = ctx.environment.getProperty(propertyName)

    /**
     * Returns all implementing bean instances of the given type (including subclasses).
     *
     * @param clazz the class or interface
     * @return Map(bean name, bean instance)
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBeansOfType(clazz: KClass<T>): Map<String, T> = ctx.getBeansOfType(clazz.java)

    /**
     * Returns all implementing bean instances of the given type (including subclasses).
     *
     * @param T the class or interface
     * @return Map(bean name, bean instance)
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> getBeansOfType(): Map<String, T> = getBeansOfType(T::class)

}