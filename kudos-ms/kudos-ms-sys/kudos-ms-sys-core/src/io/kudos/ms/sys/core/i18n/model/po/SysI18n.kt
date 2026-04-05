package io.kudos.ms.sys.core.i18n.model.po
import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * 国际化数据库实体
 *
 * @author K
 * @since 1.0.0
 */
interface SysI18n : IManagedDbEntity<String, SysI18n> {

    companion object : DbEntityFactory<SysI18n>()

    /** 语言_地区 */
    var locale: String

    /** 原子服务编码 */
    var atomicServiceCode: String

    /** 国际化类型字典代码 */
    var i18nTypeDictCode: String

    /** 国际化命名空间 */
    @get:Sortable
    var namespace: String

    /** 国际化key */
    @get:Sortable
    var key: String

    /** 国际化值 */
    var value: String

}
