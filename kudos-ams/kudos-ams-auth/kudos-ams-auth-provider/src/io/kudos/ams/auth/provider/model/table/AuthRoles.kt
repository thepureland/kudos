package io.kudos.ams.auth.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.auth.provider.model.po.AuthRole
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * 角色数据库表-实体关联对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
object AuthRoles : StringIdTable<AuthRole>("auth_role") {
//endregion your codes 1

    /** 角色编码 */
    var code = varchar("code").bindTo { it.code }

    /** 角色名称 */
    var name = varchar("name").bindTo { it.name }

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 子系统编码 */
    var subsysCode = varchar("subsys_code").bindTo { it.subsysCode }

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 是否激活 */
    var active = boolean("active").bindTo { it.active }

    /** 是否内置 */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** 创建者id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** 创建者名称 */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新者id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** 更新者名称 */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }


    //region your codes 2

    //endregion your codes 2

}
