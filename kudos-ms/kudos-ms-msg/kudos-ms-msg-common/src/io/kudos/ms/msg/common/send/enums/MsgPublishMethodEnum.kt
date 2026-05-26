package io.kudos.ms.msg.common.send.enums


/**
 * Send channel enum (matches the itemCode of the SQL dict `publish_method`).
 *
 * Also serves as the `notifyType` of [io.kudos.ability.distributed.notify.common.model.NotifyMessageVo].
 * [listenerType] gives the listener key for the current channel (e.g. `"msg.dispatch.email"`);
 * each channel's consumer (`INotifyListener`) uses it as [INotifyListener.notifyType].
 *
 * When adding a new channel:
 *   1. Add an entry here with its listenerType
 *   2. Sync the publish_method dict in V1.0.0.2__insert_sys_dict_item.sql
 *   3. Add an INotifyListener implementation in core whose notifyType() returns listenerType
 *
 * @author K
 * @since 1.0.0
 */
enum class MsgPublishMethodEnum(val dictCode: String) {

    /** Email. */
    EMAIL("email"),

    /** SMS text message. */
    SMS("sms"),

    /** In-site message. */
    SITE_MSG("siteMsg"),

    /** All users. */
    ALL_USER("all_user");

    /** Key used for [io.kudos.ability.distributed.notify.common.api.INotifyListener.notifyType]. */
    val listenerType: String get() = "msg.dispatch.$dictCode"

    companion object {
        fun fromDictCode(code: String): MsgPublishMethodEnum? = entries.firstOrNull { it.dictCode == code }
    }
}
