package io.kudos.ams.sys.service.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 门户数据库实体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface SysPortal : IDbEntity<String, SysPortal> {
//endregion your codes 1

    companion object : DbEntityFactory<SysPortal>()

    /** 编码 */
    var code: String

    /** 名称 */
    var name: String

    /** 备注 */
    var remark: String?

    /** 是否启用 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean?

    /** 创建用户 */
    var createUser: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新用户 */
    var updateUser: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}