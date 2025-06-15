package io.kudos.ability.data.rdb.jdbc.kit

import org.soul.ability.data.rdb.jdbc.metadata.RdbTypeEnum
import org.soul.ability.data.rdb.jdbc.tool.RdbTool
import org.soul.base.query.sort.Order
import java.sql.Connection
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
    fun getDataSource(): DataSource = RdbTool.getDataSource()

//    /**
//     * 取得当前上下文的数据库对象
//     *
//     * @return 当前上下文的数据库对象
//     * @author K
//     * @since 1.0.0
//     */
//    fun getDatabase(): Database = KudosContextHolder.currentDatabase()

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
    fun newConnection(url: String, username: String, password: String?): Connection =
        RdbTool.newConnection(url, username, password)

    /**
     * 测试连接是否可用
     *
     * @param conn 数据库连接。为null将用当前上下文数据源新建一个连接，在使用完关掉。不为null时由用户自行处理连接的关闭。
     * @return true: 连接可用，false: 连接不可用
     * @author K
     * @since 1.0.0
     */
    fun testConnection(conn: Connection? = null): Boolean = RdbTool.testConnection(conn)

    /**
     * 根据数据库连接url得到关系型数据库的类型
     *
     * @param url 数据库连接url
     * @return 关系型数据库的类型
     * @author K
     * @since 1.0.0
     */
    fun determinRdbTypeByUrl(url: String): RdbTypeEnum = RdbTool.determinRdbTypeByUrl(url)

    /**
     * 根据关系型数据库类型得到连接测试sql语句
     *
     * @param rdbType 关系型数据库类型
     * @return 连接测试sql语句
     * @author K
     * @since 1.0.0
     */
    fun getTestStatement(rdbType: RdbTypeEnum): String = RdbTool.getTestStatement(rdbType)


    /**
     * 返回排序规则的SQL
     *
     * @param orders 排序规则
     * @return 排序规则SQL
     * @author K
     * @since 1.0.0
     */
    fun getOrderSql(vararg orders: Order): String = RdbTool.getOrderSql(*orders)

}