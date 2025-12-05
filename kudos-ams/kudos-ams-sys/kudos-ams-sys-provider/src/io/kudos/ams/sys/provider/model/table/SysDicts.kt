package io.kudos.ams.sys.provider.model.table

import io.kudos.ams.sys.provider.model.po.SysDict
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable


/**
 * 字典数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysDicts : MaintainableTable<SysDict>("sys_dict") {
//endregion your codes 1

    /** 字典类型 */
    var dictType = varchar("dict_type").bindTo { it.dictType }

    /** 字典名称 */
    var dictName = varchar("dict_name").bindTo { it.dictName }

    /** 模块编码 */
    var moduleCode = varchar("module_code").bindTo { it.moduleCode }


    //region your codes 2

    //endregion your codes 2

}