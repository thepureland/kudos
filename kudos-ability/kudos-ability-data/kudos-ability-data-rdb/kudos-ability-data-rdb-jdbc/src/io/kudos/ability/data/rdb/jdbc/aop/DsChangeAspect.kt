package io.kudos.ability.data.rdb.jdbc.aop

import io.kudos.ability.data.rdb.jdbc.context.DbContext
import io.kudos.ability.data.rdb.jdbc.context.DbParam
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
 * @author AI: Codex
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
     * 环绕通知。写入 forcedDs / readonly，proceed 业务方法，finally 恢复进入切面前的
     * [DbParam] 快照；进入时无线程上下文则彻底 [DbContext.clear]。
     */
    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val dsChange: DsChange = signature.method.getAnnotation(DsChange::class.java)
        val previous = DbContext.getOrNull()?.copy()
        val current = previous?.copy() ?: DbParam()
        if (dsChange.value.isNotBlank()) {
            current.forcedDs = dsChange.value
        }
        current.readonly = dsChange.readonly
        DbContext.set(current)
        log.debug("强制指定数据源:ds=${current.forcedDs},readonly=${dsChange.readonly}")
        return try {
            joinPoint.proceed()
        } finally {
            restore(previous)
        }
    }

    private fun restore(previous: DbParam?) {
        if (previous == null) {
            DbContext.clear()
        } else {
            DbContext.set(previous)
        }
    }

}
