package io.kudos.context.kit

import io.kudos.base.logger.LogFactory
import org.springframework.aop.framework.AdvisedSupport
import org.springframework.aop.framework.AopProxy
import org.springframework.aop.support.AopUtils
import java.lang.reflect.Field
import kotlin.reflect.KClass

/**
 * 代理操作工具类
 *
 * @author K
 * @since 1.0.0
 */
object ProxyKit {

    private val LOG = LogFactory.getLog(this::class)

    /**
     * 取得JDK动态代理/CGLIB代理对象
     *
     * @param proxy JDK动态代理/CGLIB代理对象
     * @return 代理对象的真实类型, 如果出错将返回null
     * @author K
     * @since 1.0.0
     */
    fun getTargetClass(proxy: Any): KClass<*>? {
        if (!AopUtils.isAopProxy(proxy)) {
            return proxy::class //不是代理对象
        }
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            try {
                return getJdkDynamicProxyTargetObject(proxy)::class
            } catch (e: Exception) {
                LOG.error(e, "获取jdk动态代理对象出错！")
            }
        } else { //cglib
            try {
                return getCglibProxyTargetObject(proxy)::class
            } catch (e: Exception) {
                LOG.error(e, "获取CGLIB代理对象出错！")
            }
        }
        return null
    }

    /**
     * 从 CGLIB 生成的代理对象中反射拿到真实 target。
     *
     * 字段链：`CGLIB$CALLBACK_0` → DynamicAdvisedInterceptor → `advised` ([AdvisedSupport]) → `targetSource.target`。
     * 反射依赖 CGLIB 内部布局，Spring 升级时可能失效——届时按新版字段名调整即可。
     *
     * @param proxy CGLIB 代理对象
     * @return 被代理的真实对象
     * @throws IllegalArgumentException target 为 null（极少见，表示代理状态异常）
     * @author K
     * @since 1.0.0
     */
    private fun getCglibProxyTargetObject(proxy: Any): Any {
        val h = proxy.javaClass.getDeclaredField($$"CGLIB$CALLBACK_0")
        h.isAccessible = true
        val dynamicAdvisedInterceptor = h[proxy]
        val advised = dynamicAdvisedInterceptor.javaClass.getDeclaredField("advised")
        advised.isAccessible = true
        return requireNotNull((advised[dynamicAdvisedInterceptor] as AdvisedSupport).targetSource.target) {
            "CGLIB proxy target is null"
        }
    }

    /**
     * 从 JDK 动态代理对象中反射拿到真实 target。
     *
     * 字段链：父类 `h` 字段 ([AopProxy]) → `advised` ([AdvisedSupport]) → `targetSource.target`。
     *
     * @param proxy JDK 动态代理对象
     * @return 被代理的真实对象
     * @throws IllegalArgumentException target 为 null
     * @author K
     * @since 1.0.0
     */
    private fun getJdkDynamicProxyTargetObject(proxy: Any): Any {
        val h = proxy.javaClass.superclass.getDeclaredField("h")
        h.isAccessible = true
        val aopProxy: AopProxy = h[proxy] as AopProxy
        val advised: Field = aopProxy.javaClass.getDeclaredField("advised")
        advised.isAccessible = true
        return requireNotNull((advised[aopProxy] as AdvisedSupport).targetSource.target) {
            "JDK dynamic proxy target is null"
        }
    }

}