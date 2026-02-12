package io.kudos.ability.data.rdb.jdbc.metadata

import java.util.*

/**
 * 支持的关系型数据库类型枚举
 *
 * @author K
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
         * 返回产品名称对应的关系型数据库类型枚举
         *
         * @param productName 产品名称
         * @return 关系型数据库类型枚举
         * @author K
         * @since 1.0.0
         */
        fun ofProductName(productName: String?): RdbTypeEnum {
            return Arrays.stream(entries.toTypedArray())
                .filter { type: RdbTypeEnum? -> type != null && type.productName == productName }.findFirst().get()
        }

        /**
         * 返回JDBC驱动名称对应的关系型数据库类型枚举
         *
         * @param jdbcDriverName JDBC驱动名称
         * @return 关系型数据库类型枚举
         * @author K
         * @since 1.0.0
         */
        fun ofJdbcDriverName(jdbcDriverName: String?): RdbTypeEnum {
            return Arrays.stream(entries.toTypedArray())
                .filter { type: RdbTypeEnum? -> type != null && type.jdbcDriverName == jdbcDriverName }.findFirst().get()
        }
    }
}
