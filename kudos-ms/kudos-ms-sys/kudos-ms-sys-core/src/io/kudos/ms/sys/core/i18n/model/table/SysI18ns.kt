package io.kudos.ms.sys.core.i18n.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.i18n.model.po.SysI18n
import org.ktorm.schema.varchar


/**
 * Table-entity binding for i18n entries.
 *
 * @author K
 * @since 1.0.0
 */
object SysI18ns : ManagedTable<SysI18n>("sys_i18n") {

    /** Language_region. */
    var locale = varchar("locale").bindTo { it.locale }

    /** Atomic service code. */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }

    /** I18n type dictionary code. */
    var i18nTypeDictCode = varchar("i18n_type_dict_code").bindTo { it.i18nTypeDictCode }

    /** I18n namespace. */
    var namespace = varchar("namespace").bindTo { it.namespace }

    /** I18n key. */
    var key = varchar("key").bindTo { it.key }

    /** I18n value. */
    var value = varchar("value").bindTo { it.value }



}
