package io.kudos.ability.data.rdb.jdbc.kit

import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

/**
 * 数据源构造 / 取用工具类。
 *
 * 提供两个职能：
 *  - [getCurrentDataSource]：从上下文取当前数据源（实际是 [RdbKit.getDataSource] 的简单转发）
 *  - [createDataSource]：从 (url, user, pass) 现场新建一个 HikariCP DataSource
 *
 * **安全提醒**：[createDataSource] 不校验 / 转义入参。生产环境中如果 url 来自不可信
 * 输入，注意 JDBC connection-string 参数注入（典型如 MySQL 的 `?allowLoadLocalInfile=true`
 * 可造成文件读漏洞）。本工具默认假定调用方已对 url 做过白名单校验。
 *
 * @author K
 * @since 1.0.0
 */
object DataSourceKit {

    /** 转发到 [RdbKit.getDataSource] —— 取当前上下文数据源。 */
    fun getCurrentDataSource(): DataSource = RdbKit.getDataSource()

    /**
     * 用给定连接参数现场构造一个 [HikariDataSource]，按 url 推断 [io.kudos.ability.data.rdb.jdbc.metadata.RdbTypeEnum]
     * 并配置对应的驱动类名和 testQuery。
     *
     * 没有连接池调优 —— 用 Hikari 默认值。需要自定义 pool 大小等，调用方应自行装配。
     *
     * @param url 连接地址（**调用方应保证可信**，本方法不做注入校验）
     * @param username 用户名
     * @param password 密码（可为 null，部分数据库允许空密码）
     * @param catalog 可选 catalog
     * @param schema 可选 schema
     * @return 配好驱动类名 + testQuery 的 [HikariDataSource]
     */
    fun createDataSource(
        url: String,
        username: String,
        password: String?,
        catalog: String? = null,
        schema: String? = null
    ): DataSource {
        val rdbType = RdbKit.determinRdbTypeByUrl(url)
        return HikariDataSource().apply {
            jdbcUrl = url
            this.username = username
            this.password = password
            driverClassName = rdbType.jdbcDriverName
            connectionTestQuery = RdbKit.getTestStatement(rdbType)
            catalog?.let { this.catalog = catalog }
            schema?.let { this.schema = schema }
        }
    }

}
