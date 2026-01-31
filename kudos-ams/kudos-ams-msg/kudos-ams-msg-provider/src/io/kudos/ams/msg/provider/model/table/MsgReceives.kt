package io.kudos.ams.msg.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.msg.provider.model.po.MsgReceive
import org.ktorm.schema.*


/**
 * 消息接收数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object MsgReceives : StringIdTable<MsgReceive>("msg_receive") {
//endregion your codes 1

    /** 接收者ID */
    var receiverId = varchar("receiver_id").bindTo { it.receiverId }

    /** 发送ID */
    var sendId = varchar("send_id").bindTo { it.sendId }

    /** 接收状态字典码 */
    var receiveStatusDictCode = varchar("receive_status_dict_code").bindTo { it.receiveStatusDictCode }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }

    /** 租户ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }


    //region your codes 2

    //endregion your codes 2

}
