package io.kudos.ms.sys.core.dict.model.po
import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * 字典数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysDict : IManagedDbEntity<String, SysDict> {

    companion object : DbEntityFactory<SysDict>()

    /** 字典类型 */
    @get:Sortable
    var dictType: String

    /** 字典名称 */
    @get:Sortable
    var dictName: String

    /** 原子服务编码 */
    var atomicServiceCode: String

}