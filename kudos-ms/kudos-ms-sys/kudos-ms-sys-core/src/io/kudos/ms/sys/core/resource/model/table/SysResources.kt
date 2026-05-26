package io.kudos.ms.sys.core.resource.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.sys.core.resource.model.po.SysResource
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Resource table-to-entity binding.
 *
 * @author K
 * @since 1.0.0
 */
object SysResources : ManagedTable<SysResource>("sys_resource") {

    /** Name */
    var name = varchar("name").bindTo { it.name }

    /** url */
    var url = varchar("url").bindTo { it.url }

    /** Resource type dict code */
    var resourceTypeDictCode = varchar("resource_type_dict_code").bindTo { it.resourceTypeDictCode }

    /** Parent id */
    var parentId = varchar("parent_id").bindTo { it.parentId }

    /** Order number among siblings under the same parent */
    var orderNum = int("order_num").bindTo { it.orderNum }

    /** Icon */
    var icon = varchar("icon").bindTo { it.icon }

    /** Sub-system code */
    var subSystemCode = varchar("sub_system_code").bindTo { it.subSystemCode }




}