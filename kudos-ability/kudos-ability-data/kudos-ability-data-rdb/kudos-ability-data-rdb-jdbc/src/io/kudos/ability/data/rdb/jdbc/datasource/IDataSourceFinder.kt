package io.kudos.ability.data.rdb.jdbc.datasource

interface IDataSourceFinder {
    /**
     * 数据源配置查找器
     *
     * @param tenantId   租户id
     * @param serverCode 服务名
     * @param mode       数据库模式
     * @return
     */
    fun findDataSourceId(tenantId: String?, serverCode: String?, mode: String?): String?

}
