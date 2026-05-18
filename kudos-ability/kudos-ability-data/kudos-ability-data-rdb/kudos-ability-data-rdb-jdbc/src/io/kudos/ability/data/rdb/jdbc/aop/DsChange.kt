package io.kudos.ability.data.rdb.jdbc.aop

/**
 * 方法级数据源强制切换注解。被 [DsChangeAspect] 拦截：在方法执行前把 [value]
 * 写入当前线程的 `DbParam.forcedDs`，[readonly] 写入 `DbParam.readonly`；执行
 * 完成（成功或异常）后清空整个 `DbParam`。
 *
 * 适用场景：单个方法需要临时跑在某个特定数据源上（比如全局配置只读副本扫描），
 * 不想污染整个 service 的常规路由。
 *
 * 注意：finally 阶段 `DbContext.set(null)` 会**整体丢弃**当前线程的 `DbParam`，
 * 嵌套调用场景下里层方法返回后外层的 `DbParam` 也丢了 —— 暂不支持嵌套保留语义。
 *
 * @property value 数据源 key；空串表示不切换、仅设置 readonly 标记。
 * @property readonly true 表示只读，由切面据此选择只读副本。
 *
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DsChange(val value: String = "", val readonly: Boolean = false)
