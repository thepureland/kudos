package io.kudos.ability.log.audit.common.support

/**
 * 审计来源租户解析协议。
 *
 * 多租户场景下，业务请求里的 `tenantId` 可能与"审计日志应归属的租户"不一致——
 * 例如平台租户代操作客户租户时，审计需归属客户租户。业务侧实现本接口决定映射规则。
 *
 * @author K
 * @since 1.0.0
 */
interface ILogSourceTenantProvider {
    /**
     * 根据请求上下文中的租户与用户 id 反查"日志应归属的租户"。
     *
     * @param tenantId 请求上下文中的租户 id
     * @param userId 当前操作用户 id
     * @return 审计日志应归属的源租户 id；返回 null 表示走默认（沿用 tenantId）
     * @author K
     * @since 1.0.0
     */
    fun getSourceTenant(tenantId: String?, userId: String?): String?
}
