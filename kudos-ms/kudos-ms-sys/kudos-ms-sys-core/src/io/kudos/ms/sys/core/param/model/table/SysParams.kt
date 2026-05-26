package io.kudos.ms.sys.core.param.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.param.model.po.SysParam
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Parameter database table-entity binding object
 *
 * @author K
 * @since 1.0.0
 */
object SysParams : ManagedTable<SysParam>("sys_param") {

    /** Parameter name */
    var paramName = varchar("param_name").bindTo { it.paramName }

    /** Parameter value */
    var paramValue = varchar("param_value").bindTo { it.paramValue }

    /** Default parameter value */
    var defaultValue = varchar("default_value").bindTo { it.defaultValue }

    /** Module */
    var atomicServiceCode = varchar("atomic_service_code").bindTo { it.atomicServiceCode }

    /** Order number */
    var orderNum = int("order_num").bindTo { it.orderNum }




}