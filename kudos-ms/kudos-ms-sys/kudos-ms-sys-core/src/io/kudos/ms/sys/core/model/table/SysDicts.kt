package io.kudos.ms.sys.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.model.po.SysDict
import org.ktorm.schema.varchar


/**
 * 字典数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
object SysDicts : ManagedTable<SysDict>("sys_dict") {

    /** 字典类型 */
    var dictType = varchar("dict_type").bindTo { it.dictType }

    /** 字典名称 */
    var dictName = varchar("dict_name").bindTo { it.dictName }

    /** 原子服务编码 */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }




}