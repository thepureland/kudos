package io.kudos.ability.data.rdb.jdbc.aop

import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.base.logger.LogFactory
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component


/**
 * 处理 [DsChange] 注解的切面：方法执行前把注解参数写入 [DbContext] 的 `DbParam`，
 * 执行后整体清空 `DbParam`。
 *
 * 切面 `@Order(-100)` 比 [DynamicDataSourceAspect]（-99）更外层 —— 必须先把
 * forcedDs 写入 ThreadLocal，路由切面才看得到。
 *
 * `@Lazy` 让 bean 延迟到首次需要时才初始化，避免和早期 bean 形成循环依赖。
 *
 * @author K
 * @since 1.0.0
 */
@Component
@Aspect
@Lazy
@Order(-100)
class DsChangeAspect {

    private val log = LogFactory.getLog(this::class)

    /**
     * pointcut 定义：所有带 [DsChange] 注解的方法。
     */
    @Pointcut("@annotation(io.kudos.ability.data.rdb.jdbc.aop.DsChange)")
    private fun cut() {
    }

    /**
     * 环绕通知。写入 forcedDs / readonly，proceed 业务方法，finally 整体清空 `DbParam`
     * （`DbContext.set(null)`，不是 `clear()` —— 保留 ThreadLocal slot，下一次 `get()` 再
     * 自动创建一个空 `DbParam`）。
     *
     * 已知限制：嵌套调用场景下，内层方法 finally 把外层的 `DbParam` 也清掉了。当前实现
     * **不支持嵌套保留**。
     */
    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val dsChange: DsChange = signature.method.getAnnotation(DsChange::class.java)
        if (dsChange.value.isNotBlank()) {
            DbContext.get().forcedDs = dsChange.value
        }
        DbContext.get().readonly = dsChange.readonly
        log.debug("强制指定数据源:ds=${DbContext.get().forcedDs},readonly=${dsChange.readonly}")
        return try {
            joinPoint.proceed()
        } finally {
            DbContext.set(null)
        }
    }

}
