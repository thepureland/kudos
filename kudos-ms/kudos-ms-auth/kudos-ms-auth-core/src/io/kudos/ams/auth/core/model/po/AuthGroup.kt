package io.kudos.ms.auth.core.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 用户组数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface AuthGroup : IDbEntity<String, AuthGroup> {
//endregion your codes 1

    companion object : DbEntityFactory<AuthGroup>()

    /** 用户组编码 */
    var code: String

    /** 用户组名称 */
    var name: String

    /** 租户id */
    var tenantId: String

    /** 子系统编码 */
    var subsysCode: String

    /** 备注 */
    var remark: String?

    /** 是否激活 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean

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
