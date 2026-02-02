package io.kudos.ms.sys.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IMaintainableDbEntity

/**
 * 资源数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysResource : IMaintainableDbEntity<String, SysResource> {
//endregion your codes 1

    companion object : DbEntityFactory<SysResource>()

    /** 名称 */
    var name: String

    /** url */
    var url: String?

    /** 资源类型字典代码 */
    var resourceTypeDictCode: String

    /** 父id */
    var parentId: String?

    /** 在同父节点下的排序号 */
    var orderNum: Int?

    /** 图标 */
    var icon: String?

    /** 子系统编码 */
    var subSystemCode: String


    //region your codes 2

    //endregion your codes 2

}