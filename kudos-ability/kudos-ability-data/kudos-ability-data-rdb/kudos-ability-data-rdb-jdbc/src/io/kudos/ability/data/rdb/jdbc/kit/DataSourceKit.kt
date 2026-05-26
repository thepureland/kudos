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
 * **Security note**: [createDataSource] does not validate / escape input. In
 * production, if the url comes from untrusted input, beware of JDBC
 * connection-string parameter injection (a classic example is MySQL's
 * `?allowLoadLocalInfile=true`, which can lead to file-read vulnerabilities).
 * This utility assumes by default that callers have already allowlisted the url.
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
     * @param url connection URL (**caller must ensure trustworthiness**; this method does not validate against injection)
     * @param username username
     * @param password password (may be null; some databases permit empty passwords)
     * @param catalog optional catalog
     * @param schema optional schema
     * @return a [HikariDataSource] with driver class name and testQuery configured
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
