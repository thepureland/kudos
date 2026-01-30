package io.kudos.ams.user.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.user.provider.model.po.UserContactWay
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.ktorm.schema.datetime


/**
 * 用户联系方式数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object UserContactWays : StringIdTable<UserContactWay>("user_contact_way") {
//endregion your codes 1

    /** 用户ID */
    var userId = varchar("user_id").bindTo { it.userId }

    /** 联系方式字典码 */
    var contactWayDictCode = varchar("contact_way_dict_code").bindTo { it.contactWayDictCode }

    /** 联系方式值 */
    var contactWayValue = varchar("contact_way_value").bindTo { it.contactWayValue }

    /** 联系方式状态字典码 */
    var contactWayStatusDictCode = varchar("contact_way_status_dict_code").bindTo { it.contactWayStatusDictCode }

    /** 优先级 */
    var priority = int("priority").bindTo { it.priority }

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 是否启用 */
    var active = boolean("active").bindTo { it.active }

    /** 是否内置 */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** 创建者ID */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** 创建者名称 */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新者ID */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** 更新者名称 */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }


    //region your codes 2

    //endregion your codes 2

}
