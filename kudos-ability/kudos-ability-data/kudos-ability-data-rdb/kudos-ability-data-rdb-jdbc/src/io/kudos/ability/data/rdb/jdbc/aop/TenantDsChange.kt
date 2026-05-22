package io.kudos.ability.data.rdb.jdbc.aop

/**
 * 多租户场景下的方法级数据源切换注解，被 [TenantDsChangeAspect] 拦截。
 *
 * 与 [DsChange] 的差别：[value] 不是直接当数据源 key 用，而是当成"服务编码"，由切面
 * 包装成 `_context::<value>` 形式写入 `DbParam.forcedDs`，再由 `DynamicDataSourceAspect`
 * 根据当前租户 + 服务编码动态解析出真正的数据源 key。
 *
 * 适用场景：多租户应用里"业务方法应当走 当前租户 在 服务 X 名下的数据源"这类语义，由
 * 注解描述意图，路由查找由切面完成。
 *
 * @property value 服务编码；空串时不做任何切换。允许已带 `_context` 前缀的字符串直接透传。
 * @property readonly true 表示走只读副本。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class TenantDsChange(val value: String = "", val readonly: Boolean = false)
