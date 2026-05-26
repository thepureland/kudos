package io.kudos.ms.sys.core.locale.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.sys.core.locale.model.po.SysLocale
import io.kudos.ms.sys.core.locale.model.table.SysLocales
import org.springframework.stereotype.Repository


/**
 * Data access object for the language/locale dictionary.
 *
 * @author K
 * @since 1.0.0
 */
@Repository
open class SysLocaleDao : BaseCrudDao<String, SysLocale, SysLocales>()
