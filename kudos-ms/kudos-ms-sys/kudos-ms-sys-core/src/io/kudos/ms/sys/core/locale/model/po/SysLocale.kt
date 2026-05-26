package io.kudos.ms.sys.core.locale.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.base.query.sort.Sortable


/**
 * Entity for the language/locale dictionary.
 *
 * @author K
 * @since 1.0.0
 */
interface SysLocale : IManagedDbEntity<String, SysLocale> {

    companion object : DbEntityFactory<SysLocale>()

    /** Language code (e.g. zh_CN, en_US). */
    var code: String

    /** Display name (in the native language). */
    var displayName: String

    /** English name. */
    var englishName: String

    /** Sort order. */
    @get:Sortable
    var sortNo: Int

}
