package io.kudos.ams.auth.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.auth.provider.model.po.AuthRoleResource
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * 角色-资源关系数据库表-实体关联对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
object AuthRoleResources : StringIdTable<AuthRoleResource>("auth_role_resource") {
//endregion your codes 1

    /** 角色id */
    var roleId = varchar("role_id").bindTo { it.roleId }

    /** 资源id */
    var resourceId = varchar("resource_id").bindTo { it.resourceId }

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
