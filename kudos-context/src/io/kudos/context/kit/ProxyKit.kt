package io.kudos.context.kit

import org.soul.context.tool.ProxyTool
import kotlin.reflect.KClass

/**
 * 代理操作工具类
 *
 * @author K
 * @since 1.0.0
 */
object ProxyKit {

    /**
     * 取得JDK动态代理/CGLIB代理对象
     *
     * @param proxy JDK动态代理/CGLIB代理对象
     * @return 代理对象的真实类型, 如果出错将返回null
     * @author K
     * @since 1.0.0
     */
    fun getTargetClass(proxy: Any): KClass<*>? = ProxyTool.getTargetClass(proxy).kotlin

}