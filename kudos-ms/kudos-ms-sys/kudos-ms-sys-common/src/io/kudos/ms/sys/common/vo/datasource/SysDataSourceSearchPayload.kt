package io.kudos.ms.sys.common.vo.datasource

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 数据源查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceSearchPayload (

    //region your codes 1

    /** 名称 */
    val name: String? = null,

    /** 子系统编码 */
    val subSystemCode: String? = null,

    /** 微服务编码 */
    val microServiceCode: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** url */
    val url: String? = null,

    /** 用户名 */
    val username: String? = null,

    /** 密码 */
    val password: String? = null,

    /** 初始连接数。初始化发生在显示调用init方法，或者第一次getConnection时 */
    val initialSize: Int? = null,

    /** 最大连接数 */
    val maxActive: Int? = null,

    /** 最大空闲连接数 */
    val maxIdle: Int? = null,

    /** 最小空闲连接数。至少维持多少个空闲连接 */
    val minIdle: Int? = null,

    /** 出借最长期限(毫秒)。客户端从连接池获取（借出）一个连接后，超时没有归还（return），则连接池会抛出异常 */
    val maxWait: Int? = null,

    /** 连接寿命(毫秒)。超时(相对于初始化时间)连接池将在出借或归还时删除这个连接 */
    val maxAge: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysDataSourceRecord::class

    //endregion your codes 3

}
