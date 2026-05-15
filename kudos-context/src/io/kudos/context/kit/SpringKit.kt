package io.kudos.context.kit

import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

/**
 * spring工具类
 *
 * @author K
 * @since 1.0.0
 */
object SpringKit {

    /**
     * Spring 应用上下文。由 [io.kudos.context.spring.SpringContextInitializer] 在 Spring
     * 启动期注入。
     *
     * 标 `@Volatile`：写发生在主线程（Spring 启动期），读可能来自任意业务线程。
     * 不加 volatile 时跨线程读不保证看到写——尤其是某些 `object` 的 lazy init 在不同线程
     * 第一次访问会出现"明明已 init 完成、却读到 null"的诡异现象。
     */
    @Volatile
    var applicationContext: ApplicationContext? = null
        get() = field ?: error("Spring applicationContext is not initialized yet!")


    /**
     * 返回指定名称的Spring Bean对象，不存在会抛BeansException异常
     *
     * @param beanName bean名称
     * @return Spring Bean对象
     * @author K
     * @since 1.0.0
     */
    fun getBean(beanName: String): Any = requireNotNull(applicationContext) { "applicationContext is not initialized" }.getBean(beanName)

    /**
     * 返回指定名称的Spring Bean对象，不存在返回null
     *
     * @param beanName bean名称
     * @return Spring Bean对象
     * @author K
     * @since 1.0.0
     */
    fun getBeanOrNull(beanName: String): Any? {
        val ctx = requireNotNull(applicationContext) { "applicationContext is not initialized" }
        val exists = ctx.containsBean(beanName)
        return if (exists) ctx.getBean(beanName) else null
    }

    /**
     * 返回指定类的Spring Bean对象，不存在会抛BeansException异常
     *
     * @param T bean类型
     * @param beanClass bean类
     * @return Spring Bean对象
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBean(beanClass: KClass<T>): T = requireNotNull(applicationContext) { "applicationContext is not initialized" }.getBean(beanClass.java)

    /**
     * 返回指定泛型类型的Spring Bean对象，不存在会抛BeansException异常
     *
     * @param T bean类型
     * @return Spring Bean对象
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> getBean(): T = getBean(T::class)

    /**
     * 返回指定类的Spring Bean对象，不存在返回null
     *
     * @param T bean类型
     * @param beanClass bean类
     * @return Spring Bean对象
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBeanOrNull(beanClass: KClass<T>): T? {
        val ctx = requireNotNull(applicationContext) { "applicationContext is not initialized" }
        val exists = ctx.getBeanNamesForType(beanClass.java).isNotEmpty()
        return if (exists) ctx.getBean(beanClass.java) else null
    }
    /**
     * 返回指定名称的属性值
     *
     * @param propertyName 属性名
     * @return 属性值
     * @author K
     * @since 1.0.0
     */
    fun getProperty(propertyName: String): String? = requireNotNull(applicationContext) { "applicationContext is not initialized" }.environment.getProperty(propertyName)

    /**
     * 返回指定类型的所有实现bean实例（包括子类）
     *
     * @param clazz 类或接口
     * @return Map(bean名称, bean实例)
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBeansOfType(clazz: KClass<T>): Map<String, T> = requireNotNull(applicationContext) { "applicationContext is not initialized" }.getBeansOfType(clazz.java)

    /**
     * 返回指定类型的所有实现bean实例（包括子类）
     *
     * @param T 类或接口
     * @return Map(bean名称, bean实例)
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> getBeansOfType(): Map<String, T> = getBeansOfType(T::class)

}