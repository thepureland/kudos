package io.kudos.ability.data.rdb.jdbc.kit

import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

/**
 * Utility class for constructing and accessing data sources.
 *
 * Provides two responsibilities:
 *  - [getCurrentDataSource]: fetches the current data source from the context
 *    (essentially a simple forwarder over [RdbKit.getDataSource]).
 *  - [createDataSource]: constructs a HikariCP DataSource on the fly from
 *    (url, user, pass).
 *
 * **Security note**: [createDataSource] runs the url through [JdbcUrlValidator], which refuses
 * connection parameters that would let a connection string read local files or execute code on
 * this host (MySQL `allowLoadLocalInfile`, PostgreSQL `socketFactory`, H2 `INIT`, …). It does
 * **not** restrict which hosts are reachable — that remains a deployment-level concern.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object DataSourceKit {

    /** Forwards to [RdbKit.getDataSource] — returns the current context data source. */
    fun getCurrentDataSource(): DataSource = RdbKit.getDataSource()

    /**
     * Constructs a [HikariDataSource] on the fly using the given connection parameters,
     * infers the [io.kudos.ability.data.rdb.jdbc.metadata.RdbTypeEnum] from the url,
     * and configures the corresponding driver class name and testQuery.
     *
     * Does no connection-pool tuning — uses Hikari defaults. Callers that need
     * custom pool sizes etc. should wire those themselves.
     *
     * @param url connection URL; rejected by [JdbcUrlValidator] if it carries an unsafe connection parameter
     * @param username username
     * @param password password (may be null; some databases permit empty passwords)
     * @param catalog optional catalog
     * @param schema optional schema
     * @return a [HikariDataSource] with driver class name and testQuery configured
     * @throws IllegalArgumentException if the url carries a denied connection parameter
     */
    fun createDataSource(
        url: String,
        username: String,
        password: String?,
        catalog: String? = null,
        schema: String? = null
    ): DataSource {
        JdbcUrlValidator.validate(url)
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
