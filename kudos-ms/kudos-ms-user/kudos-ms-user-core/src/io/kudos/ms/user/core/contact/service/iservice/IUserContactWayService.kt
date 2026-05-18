package io.kudos.ms.user.core.contact.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.core.contact.model.po.UserContactWay


/**
 * 用户联系方式业务接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IUserContactWayService : IBaseCrudService<String, UserContactWay> {

    /**
     * 查询多个用户在指定联系方式类型下的"启用且优先级最高"的取值。
     *
     * 用于消息发送链路：发邮件 / 短信前，按用户 id 批量拿邮箱 / 手机号。
     * - `contactWayDictCode` 取值参考 SQL 字典 `contact_way`，如 `"201"` 表示 email。
     * - 同一用户挂多个同类型联系方式时取 `priority ASC` 第一条（priority 越小越优先；null 排最后）。
     * - 仅 `active = true` 的记录参与，避免给已禁用的联系方式发消息。
     *
     * @return Map<userId, contactWayValue>；用户没有该类型可用联系方式时不在 map 中
     */
    fun getActiveContactValuesByUserIds(
        userIds: Collection<String>,
        contactWayDictCode: String,
    ): Map<String, String>

}
