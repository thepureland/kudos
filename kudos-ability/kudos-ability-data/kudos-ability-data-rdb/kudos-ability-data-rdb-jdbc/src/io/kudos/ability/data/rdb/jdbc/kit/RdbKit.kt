package io.kudos.ability.data.rdb.jdbc.kit

import io.kudos.ability.data.rdb.jdbc.metadata.RdbTypeEnum
import io.kudos.base.lang.string.deleteWhitespace
import io.kudos.base.lang.string.substringBetween
import io.kudos.base.query.sort.Order
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource

/**
 * Common utility set for relational databases.
 *
 * Includes high-frequency operations such as fetching the current data source from
 * [io.kudos.context.core.KudosContextHolder], inferring RDB type from a url,
 * creating new JDBC connections, testing connection liveness, and producing
 * ORDER BY clauses. All methods are pure functions or thin wrappers around a single
 * DataSource, with no internal state.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object RdbKit {

    /**
     * Returns the data source from the current context.
     *
     * @return the current context data source
     * @author K
     * @since 1.0.0
     */
    fun getDataSource(): DataSource =
        (KudosContextHolder.get().otherInfos?.get(KudosContext.OTHER_INFO_KEY_DATA_SOURCE) as DataSource?)
            ?: (SpringKit.getBean("dataSource") as DataSource)

    /**
     * Returns the database object from the current context.
     *
     * @return the current context database object
     * @author K
     * @since 1.0.0
     */
//    fun getDatabase(): Database {
//        val database = KudosContextHolder.get().otherInfos?.get(KudosContext.OTHER_INFO_KEY_DATA_SOURCE) as Database?
//        return database
//    }

    /**
     * Creates a new data source connection.
     *
     * @param url connection url
     * @param username connection username
     * @param password connection password
     * @return the newly created connection
     * @author K
     * @since 1.0.0
     */
    fun newConnection(url: String, username: String, password: String?): Connection {
        val rdbType = determinRdbTypeByUrl(url)
        Class.forName(rdbType.jdbcDriverName)
        return DriverManager.getConnection(url, username, password)
    }

    /**
     * Tests whether the connection is usable.
     *
     * @param conn database connection. When null, a new connection is created from
     *   the current context data source and closed after use. When non-null, the
     *   caller is responsible for closing the connection.
     * @return true: connection is usable; false: connection is not usable
     * @author K
     * @since 1.0.0
     */
    fun testConnection(conn: Connection? = null): Boolean =
        if (conn != null) _testConnection(conn)
        else getDataSource().connection.use { _testConnection(it) }

    /**
     * Internal implementation: infers RDB type from connection metadata -> selects the
     * matching test SQL -> executes it. The statement is **not explicitly closed** —
     * historical behavior; it will be released when the caller-held connection closes.
     */
    private fun _testConnection(conn: Connection): Boolean {
        val dbMetaData = conn.metaData
        val rdbType = RdbTypeEnum.ofProductName(dbMetaData.databaseProductName)
        val statement = conn.createStatement()
        return statement.execute(getTestStatement(rdbType))
    }

    /**
     * Determines the relational database type from the connection url.
     *
     * @param url database connection url
     * @return the relational database type
     * @author K
     * @since 1.0.0
     */
    fun determinRdbTypeByUrl(url: String): RdbTypeEnum {
        if (":sqlserver:" in url.deleteWhitespace().lowercase()) return RdbTypeEnum.SQLSERVER
        return RdbTypeEnum.valueOf(url.substringBetween("jdbc:", ":").uppercase())
    }

    /**
     * Determines the relational database type from the data source.
     *
     * Parses the database type by reading the URL from the data source connection's
     * metadata. When the passed `dataSource` is null, uses the current context data
     * source (obtained via getDataSource()).
     *
     * ## Core flow
     * 1. If `dataSource` is null, use getDataSource() to fetch the current context data source.
     * 2. Acquire DatabaseMetaData from a connection to the data source.
     * 3. Read the connection URL from DatabaseMetaData.
     * 4. Call determinRdbTypeByUrl() to parse the URL and return the matching RdbTypeEnum.
     *
     * ## Dependencies and external interactions
     * - Dependency: DataSource (passed as a parameter or fetched from the context).
     * - IO: acquires a database connection (opens one to read metadata).
     *
     * ## Inputs / contract
     * - Input: `dataSource` may be null; when null, the current context data source is used.
     * - Output: an RdbTypeEnum value representing the database type (e.g. H2, MYSQL, POSTGRESQL).
     * - Errors: may throw if the data source connection fails or the URL is malformed.
     *
     * ## Performance characteristics
     * - Opens a database connection to read metadata, so has some overhead.
     * - The connection is closed automatically after the method finishes (via connection.use).
     *
     * @param dataSource data source; when null, the current context data source is used
     * @return the relational database type enum
     * @author K
     * @since 1.0.0
     */
    fun determineRdbTypeByDataSource(dataSource: DataSource?): RdbTypeEnum =
        (dataSource ?: getDataSource()).connection.use { determinRdbTypeByUrl(it.metaData.url) }

    /**
     * Returns the connection-test SQL statement for the given relational database type.
     *
     * @param rdbType relational database type
     * @return connection-test SQL statement
     * @author K
     * @since 1.0.0
     */
    fun getTestStatement(rdbType: RdbTypeEnum): String =
        when (rdbType) {
            RdbTypeEnum.ORACLE -> "select 1 from dual"
            RdbTypeEnum.DB2 -> "select 1 from sysibm.sysdummy1"
            else -> "select 1"
        }

    /**
     * Returns the SQL for the given sort orders.
     *
     * @param orders sort orders
     * @return SQL for the sort orders
     * @author K
     * @since 1.0.0
     */
    fun getOrderSql(vararg orders: Order): String =
        orders.filter { it.property.isNotBlank() && '\'' !in it.property }
            .joinToString(",", prefix = "ORDER BY ") { "${it.property} ${it.direction.name}" }
            .takeIf { it != "ORDER BY " } ?: ""

}