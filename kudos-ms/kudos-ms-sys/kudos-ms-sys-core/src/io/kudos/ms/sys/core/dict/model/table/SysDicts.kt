package io.kudos.ms.sys.core.dict.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.dict.model.po.SysDict
import org.ktorm.schema.varchar


/**
 * Dictionary database table-entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
object SysDicts : ManagedTable<SysDict>("sys_dict") {

    /** Dictionary type */
    var dictType = varchar("dict_type").bindTo { it.dictType }

    /** Dictionary name */
    var dictName = varchar("dict_name").bindTo { it.dictName }

    /** Atomic service code */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }




}
