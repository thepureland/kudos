package io.kudos.ms.msg.common.send.enums


/**
 * 发送渠道枚举（对应 SQL 字典 `publish_method` 的 itemCode）。
 *
 * 同时作为 [io.kudos.ability.distributed.notify.common.model.NotifyMessageVo] 的
 * `notifyType` —— [listenerType] 给出当前渠道的 listener key（如 `"msg.dispatch.email"`），
 * 每个渠道的 consumer (`INotifyListener`) 用它做 [INotifyListener.notifyType]。
 *
 * 加新渠道时：
 *   1. 在这里加一项 + listenerType
 *   2. 同步 V1.0.0.2__insert_sys_dict_item.sql 的 publish_method 字典
 *   3. 在 core 加一个 INotifyListener 实现，notifyType() 返回 listenerType
 *
 * @author K
 * @since 1.0.0
 */
enum class MsgPublishMethodEnum(val dictCode: String) {

    /** 电子邮件 */
    EMAIL("email"),

    /** 手机短信 */
    SMS("sms"),

    /** 站内信 */
    SITE_MSG("siteMsg"),

    /** 所有用户 */
    ALL_USER("all_user");

    /** 用于 [io.kudos.ability.distributed.notify.common.api.INotifyListener.notifyType] 的 key */
    val listenerType: String get() = "msg.dispatch.$dictCode"

    companion object {
        fun fromDictCode(code: String): MsgPublishMethodEnum? = entries.firstOrNull { it.dictCode == code }
    }
}
