package io.kudos.ms.sys.common.datasource.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 数据源缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceCacheEntry (

    /** 主键 */
    override val id: String,


    /** 名称 */
    val name: String,

    /** 子系统编码 */
    val subSystemCode: String,

    /** 微服务编码 */
    val microServiceCode: String,

    /** 租户id */
    val tenantId: String?,

    /** url */
    val url: String,

    /** 用户名 */
    val username: String,

    /** 密码 */
    val password: String?,

    /** 初始连接数。初始化发生在显示调用init方法，或者第一次getConnection时 */
    val initialSize: Int?,

    /** 最大连接数 */
    val maxActive: Int?,

    /** 最大空闲连接数 */
    val maxIdle: Int?,

    /** 最小空闲连接数。至少维持多少个空闲连接 */
    val minIdle: Int?,

    /** 出借最长期限(毫秒)。客户端从连接池获取（借出）一个连接后，超时没有归还（return），则连接池会抛出异常 */
    val maxWait: Int?,

    /** 连接寿命(毫秒)。超时(相对于初始化时间)连接池将在出借或归还时删除这个连接 */
    val maxAge: Int?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean?,

    /** 是否内置 */
    val builtIn: Boolean?,

) : IIdEntity<String>, Serializable {



    companion object {
        private const val serialVersionUID = 5789474834656195376L
    }

}
