package io.kudos.ams.sys.common.vo.datasource

import io.kudos.base.support.payload.FormPayload


/**
 * 数据源表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourcePayload (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 名称 */
    var name: String? = null,

    /** 子系统编码 */
    var subSystemCode: String? = null,

    /** 微服务编码 */
    var microServiceCode: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** url */
    var url: String? = null,

    /** 用户名 */
    var username: String? = null,

    /** 密码 */
    var password: String? = null,

    /** 初始连接数。初始化发生在显示调用init方法，或者第一次getConnection时 */
    var initialSize: Int? = null,

    /** 最大连接数 */
    var maxActive: Int? = null,

    /** 最大空闲连接数 */
    var maxIdle: Int? = null,

    /** 最小空闲连接数。至少维持多少个空闲连接 */
    var minIdle: Int? = null,

    /** 出借最长期限(毫秒)。客户端从连接池获取（借出）一个连接后，超时没有归还（return），则连接池会抛出异常 */
    var maxWait: Int? = null,

    /** 连接寿命(毫秒)。超时(相对于初始化时间)连接池将在出借或归还时删除这个连接 */
    var maxAge: Int? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}