package io.kudos.context.kit

import io.kudos.base.logger.LogFactory
import org.springframework.aop.framework.AdvisedSupport
import org.springframework.aop.framework.AopProxy
import org.springframework.aop.support.AopUtils
import java.lang.reflect.Field
import kotlin.reflect.KClass

/**
 * Utility for proxy operations.
 *
 * @author K
 * @since 1.0.0
 */
object ProxyKit {

    private val LOG = LogFactory.getLog(this::class)

    /**
     * Obtains the real target class of a JDK dynamic proxy / CGLIB proxy object.
     *
     * @param proxy a JDK dynamic proxy or CGLIB proxy object
     * @return the real type of the proxy target, or null on error
     * @author K
     * @since 1.0.0
     */
    fun getTargetClass(proxy: Any): KClass<*>? {
        if (!AopUtils.isAopProxy(proxy)) {
            return proxy::class // not a proxy object
        }
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            try {
                return getJdkDynamicProxyTargetObject(proxy)::class
            } catch (e: Exception) {
                LOG.error(e, "failed to obtain the JDK dynamic proxy target object")
            }
        } else { // cglib
            try {
                return getCglibProxyTargetObject(proxy)::class
            } catch (e: Exception) {
                LOG.error(e, "failed to obtain the CGLIB proxy target object")
            }
        }
        return null
    }

    /**
     * Reflectively obtains the real target from a CGLIB-generated proxy object.
     *
     * Field chain: `CGLIB$CALLBACK_0` -> DynamicAdvisedInterceptor -> `advised` ([AdvisedSupport]) -> `targetSource.target`.
     * The reflection relies on CGLIB's internal layout and may break on Spring upgrades — in that case, adjust to the new field names.
     *
     * @param proxy the CGLIB proxy object
     * @return the real target object being proxied
     * @throws IllegalArgumentException if the target is null (very rare, indicating an abnormal proxy state)
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
     * Reflectively obtains the real target from a JDK dynamic proxy object.
     *
     * Field chain: superclass `h` field ([AopProxy]) -> `advised` ([AdvisedSupport]) -> `targetSource.target`.
     *
     * @param proxy the JDK dynamic proxy object
     * @return the real target object being proxied
     * @throws IllegalArgumentException if the target is null
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