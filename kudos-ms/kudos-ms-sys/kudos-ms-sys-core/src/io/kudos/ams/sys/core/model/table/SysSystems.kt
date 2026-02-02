package io.kudos.ms.sys.core.model.table

import io.kudos.ms.sys.core.model.po.SysSystem
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar


/**
 * 系统数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object SysSystems : Table<SysSystem>("sys_system") {
//endregion your codes 1

    /** 编码 */
    var code = varchar("code").bindTo { it.code }

    /** 名称 */
    var name = varchar("name").bindTo { it.name }

    /** 是否子系统 */
    var subSystem = boolean("sub_system").bindTo { it.subSystem }

    /** 父系统编号 */
    var parentCode = varchar("parent_code").bindTo { it.parentCode }

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 是否启用 */
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

    /** 主键别名 */
    var id = code.primaryKey()

    //endregion your codes 2

}
