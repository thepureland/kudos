package io.kudos.ability.data.rdb.jdbc.datasource

/**
 * "按租户 + 服务 + 模式查找数据源 id"的扩展点。
 *
 * 由 [DsContextProcessor] 调用：当切面命中 `_context::<serviceCode>` 类型的动态路由时，
 * 框架不知道具体应该走哪个 dsId —— 这一步留给业务方实现。典型场景：从配置中心 / 元数据
 * 表里按 (租户, 服务, master|readonly) 查出一个具体的 dsId。
 *
 * 不实现时（容器里没有这个 bean），动态路由会回退到上下文里的 `dataSourceId`。
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDataSourceFinder {
    /**
     * 按租户 id / 服务编码 / 模式（master / readonly）解析出真正的数据源 id。
     * 返回 `null` 表示"找不到"，调用方会回退到上下文默认值。
     *
     * @param tenantId   租户 id；多租户路由的关键维度，可以为 `null` 表示无租户
     * @param serverCode 服务编码（对应 `@TenantDsChange.value`）
     * @param mode       数据库模式（`master` 或 `readonly`，见 [io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst]）
     */
    fun findDataSourceId(tenantId: String?, serverCode: String?, mode: String?): String?

}
