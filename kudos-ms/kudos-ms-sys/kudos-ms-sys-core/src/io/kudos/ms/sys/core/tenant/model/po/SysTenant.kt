package io.kudos.ms.sys.core.tenant.model.po
import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * 租户数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysTenant : IManagedDbEntity<String, SysTenant> {

    companion object : DbEntityFactory<SysTenant>()

    /** 名称 */
    @get:Sortable
    var name: String

    /** 时区 */
    var timezone: String?

    /** 默认语言编码 */
    var defaultLanguageCode: String?

}