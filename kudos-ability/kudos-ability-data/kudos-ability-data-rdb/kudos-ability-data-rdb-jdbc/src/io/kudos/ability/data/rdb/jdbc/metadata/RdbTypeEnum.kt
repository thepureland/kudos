package io.kudos.ability.data.rdb.jdbc.metadata

/**
 * Enumeration of supported relational database types.
 *
 * Each enum value binds two pieces of factual information:
 * - [productName]: the string returned by JDBC `DatabaseMetaData.getDatabaseProductName()`, used to "look up the type from an existing connection"
 * - [jdbcDriverName]: the fully qualified standard driver class name, used to "manually Class.forName-load the driver after inferring the type from the URL"
 *
 * Use [ofProductName] / [ofJdbcDriverName] for bidirectional lookup; any database type not declared here is not supported by this framework.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class RdbTypeEnum(val productName: String, val jdbcDriverName: String) {
    H2("H2", "org.h2.Driver"),
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver"),
    MARIA("MariaDB", "org.mariadb.jdbc.Driver"),
    ORACLE("Oracle", "oracle.jdbc.driver.OracleDriver"),
    SQLITE("SQLite", "org.sqlite.JDBC"),
    DB2("DB2", "com.ibm.db2.jcc.DB2Driver"),
    CLICKHOUSE("ClickHouse", "com.clickhouse.jdbc.ClickHouseDriver"),
    SQLSERVER("SqlServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver");

    companion object {
        /**
         * Returns the relational database type enum corresponding to the given product name.
         *
         * @param productName the product name
         * @return the relational database type enum
         * @author K
         * @since 1.0.0
         */
        fun ofProductName(productName: String?): RdbTypeEnum =
            entries.first { it.productName == productName }

        /**
         * Returns the relational database type enum corresponding to the given JDBC driver name.
         *
         * @param jdbcDriverName the JDBC driver name
         * @return the relational database type enum
         * @author K
         * @since 1.0.0
         */
        fun ofJdbcDriverName(jdbcDriverName: String?): RdbTypeEnum =
            entries.first { it.jdbcDriverName == jdbcDriverName }
    }
}
