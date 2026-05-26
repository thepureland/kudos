package io.kudos.ms.sys.core.locale.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.locale.model.po.SysLocale
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Table-entity binding for the language/locale dictionary.
 *
 * @author K
 * @since 1.0.0
 */
object SysLocales : ManagedTable<SysLocale>("sys_locale") {

    /** Language code. */
    var code = varchar("code").bindTo { it.code }

    /** Display name. */
    var displayName = varchar("display_name").bindTo { it.displayName }

    /** English name. */
    var englishName = varchar("english_name").bindTo { it.englishName }

    /** Sort order. */
    var sortNo = int("sort_no").bindTo { it.sortNo }

}
