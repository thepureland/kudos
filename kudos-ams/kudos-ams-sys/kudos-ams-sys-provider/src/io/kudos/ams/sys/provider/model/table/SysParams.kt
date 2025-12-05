package io.kudos.ams.sys.provider.model.table

import io.kudos.ams.sys.provider.model.po.SysParam
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable


/**
 * 参数数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysParams : MaintainableTable<SysParam>("sys_param") {
//endregion your codes 1

    /** 参数名称 */
    var paramName = varchar("param_name").bindTo { it.paramName }

    /** 参数值 */
    var paramValue = varchar("param_value").bindTo { it.paramValue }

    /** 默认参数值 */
    var defaultValue = varchar("default_value").bindTo { it.defaultValue }

    /** 模块 */
    var moduleCode = varchar("module_code").bindTo { it.moduleCode }

    /** 序号 */
    var orderNum = int("order_num").bindTo { it.orderNum }


    //region your codes 2

    //endregion your codes 2

}