package io.kudos.ms.msg.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.msg.core.model.po.MsgSend
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * 消息发送数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object MsgSends : StringIdTable<MsgSend>("msg_send") {
//endregion your codes 1

    /** 接收者群组类型字典码 */
    var receiverGroupTypeDictCode = varchar("receiver_group_type_dict_code").bindTo { it.receiverGroupTypeDictCode }

    /** 接收者群组ID */
    var receiverGroupId = varchar("receiver_group_id").bindTo { it.receiverGroupId }

    /** 消息实例ID */
    var instanceId = varchar("instance_id").bindTo { it.instanceId }

    /** 消息类型字典码 */
    var msgTypeDictCode = varchar("msg_type_dict_code").bindTo { it.msgTypeDictCode }

    /** 国家-语言字典码 */
    var localeDictCode = varchar("locale_dict_code").bindTo { it.localeDictCode }

    /** 发送状态字典码 */
    var sendStatusDictCode = varchar("send_status_dict_code").bindTo { it.sendStatusDictCode }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }

    /** 发送成功数量 */
    var successCount = int("success_count").bindTo { it.successCount }

    /** 发送失败数量 */
    var failCount = int("fail_count").bindTo { it.failCount }

    /** 定时任务ID */
    var jobId = varchar("job_id").bindTo { it.jobId }

    /** 租户ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }


    //region your codes 2

    //endregion your codes 2

}
