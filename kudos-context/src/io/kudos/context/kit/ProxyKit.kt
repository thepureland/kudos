package io.kudos.context.kit

import org.springframework.aop.framework.AopProxyUtils
import kotlin.reflect.KClass

/**
 * Utility for proxy operations.
 *
 * @author K
 * @since 1.0.0
 */
object ProxyKit {

    /**
     * Obtains the real target class behind a JDK dynamic proxy / CGLIB proxy. A non-proxy object is
     * returned as its own `obj::class`.
     *
     * The previous implementation used reflection to read `CGLIB$CALLBACK_0` / the JDK `h` field plus
     * `AdvisedSupport.targetSource.target`. Field names/layout may change after the built-in CGLib
     * rewrite in Spring Framework 6 / Spring Boot 4, which is a latent bug source. This calls the
     * public Spring API [AopProxyUtils.ultimateTargetClass] instead — it sees through nested proxies
     * to the ultimate target class, works for both JDK dynamic and CGLib proxies, and has no
     * reflection happy-path risk.
     *
     * @param proxy a proxy object or a plain object
     * @return the real target type, or null on any error path (consistent with the old contract)
     * @author K
     * @since 1.0.0
     */
    fun getTargetClass(proxy: Any): KClass<*>? = runCatching {
        AopProxyUtils.ultimateTargetClass(proxy).kotlin
    }.getOrNull()

}