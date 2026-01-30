package io.kudos.ams.user.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 用户账号第三方绑定数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface UserAccountThird : IDbEntity<String, UserAccountThird> {
//endregion your codes 1

    companion object : DbEntityFactory<UserAccountThird>()

    /** 关联用户账号ID */
    var userAccountId: String

    /** 第三方平台字典码 */
    var accountProviderDictCode: String

    /** 发行方/平台租户 */
    var providerIssuer: String?

    /** 第三方用户唯一标识 */
    var subject: String

    /** 跨应用统一标识 */
    var unionId: String?

    /** 第三方展示名 */
    var externalDisplayName: String?

    /** 第三方邮箱 */
    var externalEmail: String?

    /** 头像URL */
    var avatarUrl: String?

    /** 最后登录时间 */
    var lastLoginTime: LocalDateTime?

    /** 子系统代码 */
    var subSysDictCode: String

    /** 租户ID */
    var tenantId: String

    /** 备注 */
    var remark: String?

    /** 是否激活 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean

    /** 创建用户 */
    var createUser: String?

    /** 创建时间 */
    var createTime: LocalDateTime

    /** 更新用户 */
    var updateUser: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}
