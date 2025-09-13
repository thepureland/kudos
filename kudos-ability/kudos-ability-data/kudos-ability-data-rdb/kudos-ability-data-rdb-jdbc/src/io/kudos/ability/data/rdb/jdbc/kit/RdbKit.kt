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
 * 关系型数据库操作工具类
 *
 * @author K
 * @since 1.0.0
 */
object RdbKit {

    /**
     * 取得当前上下文的数据源
     *
     * @return 当前上下文的数据源
     * @author K
     * @since 1.0.0
     */
    fun getDataSource(): DataSource {
        val dataSource = KudosContextHolder.get().otherInfos?.get(KudosContext.OTHER_INFO_KEY_DATA_SOURCE) as DataSource?
        return dataSource ?: SpringKit.getBean("dataSource") as DataSource
    }

    /**
     * 取得当前上下文的数据库对象
     *
     * @return 当前上下文的数据库对象
     * @author K
     * @since 1.0.0
     */
//    fun getDatabase(): Database {
//        val database = KudosContextHolder.get().otherInfos?.get(KudosContext.OTHER_INFO_KEY_DATA_SOURCE) as Database?
//        return database
//    }

    /**
     * 新建一个数据源连接
     *
     * @param url 连接url
     * @param username 连接用户名
     * @param password 连接密码
     * @return 新建的连接
     * @author K
     * @since 1.0.0
     */
    fun newConnection(url: String, username: String, password: String?): Connection {
        val rdbType = determinRdbTypeByUrl(url)
        Class.forName(rdbType.jdbcDriverName)
        return DriverManager.getConnection(url, username, password)
    }

    /**
     * 测试连接是否可用
     *
     * @param conn 数据库连接。为null将用当前上下文数据源新建一个连接，在使用完关掉。不为null时由用户自行处理连接的关闭。
     * @return true: 连接可用，false: 连接不可用
     * @author K
     * @since 1.0.0
     */
    fun testConnection(conn: Connection? = null): Boolean {
        return if (conn != null) {
            _testConnection(conn)
        } else {
            getDataSource().connection.use {
                _testConnection(it)
            }
        }
    }

    private fun _testConnection(conn: Connection): Boolean {
        val dbMetaData = conn.metaData
        val rdbType = RdbTypeEnum.ofProductName(dbMetaData.databaseProductName)
        val statement = conn.createStatement()
        return statement.execute(getTestStatement(rdbType))
    }

    /**
     * 根据数据库连接url得到关系型数据库的类型
     *
     * @param url 数据库连接url
     * @return 关系型数据库的类型
     * @author K
     * @since 1.0.0
     */
    fun determinRdbTypeByUrl(url: String): RdbTypeEnum {
        val urlStr = url.deleteWhitespace().lowercase()
        if (urlStr.contains(":sqlserver:"))
            return RdbTypeEnum.SQLSERVER
        val type = url.substringBetween("jdbc:", ":")
        return RdbTypeEnum.valueOf(type.uppercase())
    }

    /**
     * 根据关系型数据库类型得到连接测试sql语句
     *
     * @param rdbType 关系型数据库类型
     * @return 连接测试sql语句
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
     * 返回排序规则的SQL
     *
     * @param orders 排序规则
     * @return 排序规则SQL
     * @author K
     * @since 1.0.0
     */
    fun getOrderSql(vararg orders: Order): String {
        var orderStr = ""
        val orderSb = StringBuilder("ORDER BY ")
        val length = orderSb.length
        orders.forEach {
            if (it.property.isNotBlank() && !it.property.contains("'")) {
                orderSb.append("${it.property} ${it.direction.name},")
            }
        }
        if (orderSb.length != length) {
            orderStr = orderSb.deleteCharAt(orderSb.lastIndex).toString()
        }
        return orderStr
    }

}