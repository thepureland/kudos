package io.kudos.ms.sys.core.i18n.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable

/**
 * Database entity for i18n entries.
 *
 * @author K
 * @since 1.0.0
 */
interface SysI18n : IManagedDbEntity<String, SysI18n> {

    companion object : DbEntityFactory<SysI18n>()

    /** Language_region. */
    var locale: String

    /** Atomic service code. */
    var atomicServiceCode: String

    /** I18n type dictionary code. */
    var i18nTypeDictCode: String

    /** I18n namespace. */
    @get:Sortable
    var namespace: String

    /** I18n key. */
    @get:Sortable
    var key: String

    /** I18n value. */
    var value: String

}
