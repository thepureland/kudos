package io.kudos.ams.user.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.user.provider.model.po.AuthDept
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * 部门数据库表-实体关联对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
object AuthDepts : StringIdTable<AuthDept>("auth_dept") {
//endregion your codes 1

    /** 部门名称 */
    var name = varchar("name").bindTo { it.name }

    /** 部门简称 */
    var shortName = varchar("short_name").bindTo { it.shortName }

    /** 租户id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 父部门id */
    var parentId = varchar("parent_id").bindTo { it.parentId }

    /** 部门类型字典码 */
    var deptTypeDictCode = varchar("dept_type_dict_code").bindTo { it.deptTypeDictCode }

    /** 排序号 */
    var sortNum = int("sort_num").bindTo { it.sortNum }

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
