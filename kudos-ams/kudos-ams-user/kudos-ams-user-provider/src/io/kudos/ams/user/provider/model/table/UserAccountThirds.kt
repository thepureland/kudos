package io.kudos.ams.user.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.user.provider.model.po.UserAccountThird
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * 用户账号第三方绑定数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object UserAccountThirds : StringIdTable<UserAccountThird>("user_account_third") {
//endregion your codes 1

    /** 关联用户账号ID */
    var userAccountId = varchar("user_account_id").bindTo { it.userAccountId }

    /** 第三方平台字典码 */
    var accountProviderDictCode = varchar("account_provider_dict_code").bindTo { it.accountProviderDictCode }

    /** 发行方/平台租户 */
    var providerIssuer = varchar("provider_issuer").bindTo { it.providerIssuer }

    /** 第三方用户唯一标识 */
    var subject = varchar("subject").bindTo { it.subject }

    /** 跨应用统一标识 */
    var unionId = varchar("union_id").bindTo { it.unionId }

    /** 第三方展示名 */
    var externalDisplayName = varchar("external_display_name").bindTo { it.externalDisplayName }

    /** 第三方邮箱 */
    var externalEmail = varchar("external_email").bindTo { it.externalEmail }

    /** 头像URL */
    var avatarUrl = varchar("avatar_url").bindTo { it.avatarUrl }

    /** 最后登录时间 */
    var lastLoginTime = datetime("last_login_time").bindTo { it.lastLoginTime }

    /** 子系统代码 */
    var subSysDictCode = varchar("sub_sys_dict_code").bindTo { it.subSysDictCode }

    /** 租户ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 是否激活 */
    var active = boolean("active").bindTo { it.active }

    /** 是否内置 */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** 创建用户 */
    var createUser = varchar("create_user").bindTo { it.createUser }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新用户 */
    var updateUser = varchar("update_user").bindTo { it.updateUser }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }


    //region your codes 2

    //endregion your codes 2

}
