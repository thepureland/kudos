package io.kudos.ability.data.rdb.jdbc.datasource

import com.baomidou.dynamic.datasource.creator.DataSourceProperty

/**
 * "按 dsId 加载 baomidou [DataSourceProperty]"的扩展点。
 *
 * [DsContextProcessor.getDatasourceKey] 在发现当前路由表里缺某个 dsId 对应的真实
 * DataSource 时，回头来这里要"这个 dsId 的连接信息（url/user/pass/driver/...）"，
 * 据此现场创建并注册到动态路由表里。典型用法：从配置中心 / 元数据库按 dsId 拉配置。
 *
 * 不实现时使用 [DefaultDynamicDataSourceLoad]，所有 dsId 都返回 null —— 等同于"动
 * 态数据源加载未启用"，路由命中未配置项时会失败。
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDynamicDataSourceLoad {
    /**
     * 按数据源 id 返回 baomidou [DataSourceProperty]；找不到返回 `null`。
     */
    fun getPropertyById(dsId: String?): DataSourceProperty?
}
