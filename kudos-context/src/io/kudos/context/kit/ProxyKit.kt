package io.kudos.context.kit

import org.springframework.aop.framework.AopProxyUtils
import kotlin.reflect.KClass

/**
 * 代理操作工具类
 *
 * @author K
 * @since 1.0.0
 */
object ProxyKit {

    /**
     * 取得 JDK 动态代理 / CGLIB 代理背后的真实目标类。非代理对象原样返回 `obj::class`。
     *
     * 旧实现走反射读 `CGLIB$CALLBACK_0` / JDK `h` 字段 + `AdvisedSupport.targetSource.target`，
     * 在 Spring Framework 6 / Spring Boot 4 内置 CGLib 重写后字段名/结构可能变，是潜在 bug
     * 来源。改为调 Spring 公开 API [AopProxyUtils.ultimateTargetClass]——它穿透嵌套代理拿到
     * 最终目标类，对 JDK 动态代理与 CGLib 代理都成立，且没有反射的 happy-path 风险。
     *
     * @param proxy 代理对象或普通对象
     * @return 真实目标类型；任何异常路径返回 null（与旧契约一致）
     * @author K
     * @since 1.0.0
     */
    fun getTargetClass(proxy: Any): KClass<*>? = runCatching {
        AopProxyUtils.ultimateTargetClass(proxy).kotlin
    }.getOrNull()

}