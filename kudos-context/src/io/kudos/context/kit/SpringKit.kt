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
    fun getBean(beanName: String): Any = applicationContext!!.getBean(beanName)

    /**
     * 返回指定名称的Spring Bean对象，不存在返回null
     *
     * @param beanName bean名称
     * @return Spring Bean对象
     * @author K
     * @since 1.0.0
     */
    fun getBeanOrNull(beanName: String): Any? {
        val exists = applicationContext!!.containsBean(beanName)
        return if (exists) applicationContext!!.getBean(beanName) else null
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
    fun <T : Any> getBean(beanClass: KClass<T>): T = applicationContext!!.getBean(beanClass.java)

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
        val exists = applicationContext!!.getBeanNamesForType(beanClass.java).isNotEmpty()
        return if (exists) applicationContext!!.getBean(beanClass.java) else null
    }
    /**
     * 返回指定名称的属性值
     *
     * @param propertyName 属性名
     * @return 属性值
     * @author K
     * @since 1.0.0
     */
    fun getProperty(propertyName: String): String? = applicationContext!!.environment.getProperty(propertyName)

    /**
     * 返回指定类型的所有实现bean实例（包括子类）
     *
     * @param clazz 类或接口
     * @return Map(bean名称, bean实例)
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBeansOfType(clazz: KClass<T>): Map<String, T> = applicationContext!!.getBeansOfType(clazz.java)

}