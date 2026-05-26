package io.kudos.ability.data.rdb.jdbc.datasource

/**
 * Extension point for "looking up a data source id by tenant + service + mode".
 *
 * Called by [DsContextProcessor]: when the aspect hits a `_context::<serviceCode>`
 * dynamic route, the framework does not know which dsId to use — this step is
 * left to the business side. Typical scenario: look up a concrete dsId by
 * (tenant, service, master|readonly) from a configuration center / metadata table.
 *
 * When not implemented (no bean in the container), dynamic routing falls back to
 * the `dataSourceId` in the context.
 *
 * @author damon
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDataSourceFinder {
    /**
     * Resolves the real data source id by tenant id / service code / mode
     * (master / readonly). Returns `null` to indicate "not found"; the caller then
     * falls back to the context default.
     *
     * @param tenantId   tenant id; the key dimension for multi-tenant routing,
     *                   may be `null` to indicate no tenant
     * @param serverCode service code (corresponds to `@TenantDsChange.value`)
     * @param mode       database mode (`master` or `readonly`; see
     *                   [io.kudos.ability.data.rdb.jdbc.consts.DatasourceConst])
     */
    fun findDataSourceId(tenantId: String?, serverCode: String?, mode: String?): String?

}
