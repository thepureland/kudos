package io.kudos.ams.auth.provider.user.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.auth.provider.user.model.po.AuthDeptUser
import org.ktorm.schema.*


/**
 * 部门-用户关系数据库表-实体关联对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
object AuthDeptUsers : StringIdTable<AuthDeptUser>("auth_dept_user") {
//endregion your codes 1

    /** 部门id */
    var deptId = varchar("dept_id").bindTo { it.deptId }

    /** 用户id */
    var userId = varchar("user_id").bindTo { it.userId }

    /** 是否为部门管理员 */
    var deptAdmin = boolean("dept_admin").bindTo { it.deptAdmin }

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
