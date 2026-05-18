package io.kudos.ms.msg.common.send.vo.request

import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum


/**
 * 业务方调 [io.kudos.ms.msg.common.send.api.IMsgSendApi.publish] 的入参。
 *
 * 路由模型：业务方传 (tenantId, eventTypeDictCode, msgTypeDictCode, locale?)，
 * publish service 用这四元组去匹配模板；找不到模板视作配置缺失、不发送、返回 null。
 *
 * @author K
 * @since 1.0.0
 */
data class MsgPublishRequest(

    /** 租户 id */
    val tenantId: String,

    /** 事件类型字典码（如 user_registered / order_paid / password_reset） */
    val eventTypeDictCode: String,

    /** 消息类型字典码（业务自定义） */
    val msgTypeDictCode: String,

    /** 发送渠道 —— 一次 publish 走一个渠道；多渠道由调用方分别 publish */
    val publishMethod: MsgPublishMethodEnum,

    /** 接收者用户 id 集合 */
    val receiverIds: Set<String>,

    /**
     * 业务参数，将填入模板的 `${name}` 占位符。
     * 与自动参数 (time/date/year/...) 同名时业务参数胜出。
     */
    val params: Map<String, String> = emptyMap(),

    /** 语言；null 表示不限定 locale，让 publish service 拿到首条匹配 */
    val localeDictCode: String? = null,
)
