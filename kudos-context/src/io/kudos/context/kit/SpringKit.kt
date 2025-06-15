package io.kudos.context.kit

import org.soul.context.tool.SpringTool
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

/**
 * spring工具类
 *
 * 注意：使用该工具必须自行确保spring应用上下文已经初始化完成！
 *
 * @author K
 * @since 1.0.0
 */
object SpringKit {

    /**
     * 返回spring应用上下文
     *
     * @return spring应用上下文
     * @author K
     * @since 1.0.0
     */
    fun getApplicationContext(): ApplicationContext = SpringTool.getApplicationContext()

    /**
     * 返回指定名称的Spring Bean对象
     *
     * @param beanName bean名称
     * @return Spring Bean对象
     * @author K
     * @since 1.0.0
     */
    fun getBean(beanName: String): Any = SpringTool.getBean(beanName)

    /**
     * 返回指定类的Spring Bean对象
     *
     * @param T bean类型
     * @param beanClass bean类
     * @return Spring Bean对象
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBean(beanClass: KClass<T>): T = SpringTool.getBean(beanClass.java)

    /**
     * 返回指定名称的属性值
     *
     * @param propertyName 属性名
     * @return 属性值
     * @author K
     * @since 1.0.0
     */
    fun getProperty(propertyName: String): String? = getApplicationContext().environment.getProperty(propertyName)

    /**
     * 返回指定类型的所有实现bean实例（包括子类）
     *
     * @param clazz 类或接口
     * @return Map(bean名称, bean实例)
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> getBeansOfType(clazz: KClass<T>): Map<String, T> = SpringTool.getBeansOfType(clazz.java)

}