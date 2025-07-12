package io.kudos.ams.sys.common.vo.datasource

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 数据源查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysDataSourceSearchPayload : ListSearchPayload() {
//endregion your codes 1

    //region your codes 2

    /** 名称，或其国际化key */
    var name: String? = null

    /** 子系统编码 */
    var subSystemCode: String? = null

    /** 微服务编码 */
    var microServiceCode: String? = null

    /** 原子服务编码 */
    var atomicServiceCode: String? = null

    /** 租户id */
    var tenantId: String? = null

    /** url */
    var url: String? = null

    /** 用户名 */
    var username: String? = null

    /** 密码，强烈建议加密 */
    var password: String? = null

    /** 是否启用 */
    var active: Boolean? = null


    //endregion your codes 2

    override var returnEntityClass: KClass<*>? = SysDataSourceRecord::class

}