package io.kudos.ms.sys.core.param.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * 参数数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysParam : IManagedDbEntity<String, SysParam> {

    companion object : DbEntityFactory<SysParam>()

    /** 参数名称 */
    @get:Sortable
    var paramName: String

    /** 参数值 */
    var paramValue: String

    /** 默认参数值 */
    var defaultValue: String?

    /** 原子服务编码 */
    var atomicServiceCode: String

    /** 序号 */
    var orderNum: Int?

}