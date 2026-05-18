package io.kudos.ability.data.rdb.jdbc.metadata

/**
 * 支持的关系型数据库类型枚举。
 *
 * 每个枚举值绑定两个事实信息：
 * - [productName]：JDBC `DatabaseMetaData.getDatabaseProductName()` 返回的字符串，用于"从已有连接反查类型"
 * - [jdbcDriverName]：标准驱动类的全限定名，用于"从 url 推断类型后手动 Class.forName 加载驱动"
 *
 * 通过 [ofProductName] / [ofJdbcDriverName] 双向查找；任何未在此声明的数据库类型都不被本框架支持。
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
        fun ofProductName(productName: String?): RdbTypeEnum =
            entries.first { it.productName == productName }

        /**
         * 返回JDBC驱动名称对应的关系型数据库类型枚举
         *
         * @param jdbcDriverName JDBC驱动名称
         * @return 关系型数据库类型枚举
         * @author K
         * @since 1.0.0
         */
        fun ofJdbcDriverName(jdbcDriverName: String?): RdbTypeEnum =
            entries.first { it.jdbcDriverName == jdbcDriverName }
    }
}
