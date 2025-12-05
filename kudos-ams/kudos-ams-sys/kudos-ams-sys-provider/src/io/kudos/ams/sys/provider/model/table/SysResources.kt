package io.kudos.ams.sys.provider.model.table

import io.kudos.ams.sys.provider.model.po.SysResource
import org.ktorm.schema.*
import io.kudos.ability.data.rdb.ktorm.support.MaintainableTable


/**
 * 资源数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysResources : MaintainableTable<SysResource>("sys_resource") {
//endregion your codes 1

    /** 名称 */
    var name = varchar("name").bindTo { it.name }

    /** url */
    var url = varchar("url").bindTo { it.url }

    /** 资源类型字典代码 */
    var resourceTypeDictCode = varchar("resource_type_dict_code").bindTo { it.resourceTypeDictCode }

    /** 父id */
    var parentId = varchar("parent_id").bindTo { it.parentId }

    /** 在同父节点下的排序号 */
    var orderNum = int("order_num").bindTo { it.orderNum }

    /** 图标 */
    var icon = varchar("icon").bindTo { it.icon }

    /** 子系统编码 */
    var subSystemCode = varchar("sub_system_code").bindTo { it.subSystemCode }


    //region your codes 2

    //endregion your codes 2

}