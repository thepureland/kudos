package io.kudos.ms.user.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 机构数据库实体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface UserOrg : IDbEntity<String, UserOrg> {
//endregion your codes 1

    companion object : DbEntityFactory<UserOrg>()

    /** 机构名称 */
    var name: String

    /** 机构简称 */
    var shortName: String?

    /** 租户id */
    var tenantId: String

    /** 父机构id */
    var parentId: String?

    /** 机构类型字典码 */
    var orgTypeDictCode: String

    /** 排序号 */
    var sortNum: Int?

    /** 备注 */
    var remark: String?

    /** 是否激活 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean?

    /** 创建者id */
    var createUserId: String?

    /** 创建者名称 */
    var createUserName: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新者id */
    var updateUserId: String?

    /** 更新者名称 */
    var updateUserName: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}
