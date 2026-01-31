package io.kudos.ams.user.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.user.provider.model.po.UserLoginRememberMe
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * 记住我登录数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object UserLoginRememberMes : StringIdTable<UserLoginRememberMe>("user_login_remember_me") {
//endregion your codes 1

    /** 用户ID */
    var userId = varchar("user_id").bindTo { it.userId }

    /** 租户ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 用户名 */
    var username = varchar("username").bindTo { it.username }

    /** 令牌 */
    var token = varchar("token").bindTo { it.token }

    /** 最后使用时间 */
    var lastUsed = datetime("last_used").bindTo { it.lastUsed }


    //region your codes 2

    //endregion your codes 2

}
