package io.kudos.ms.msg.core.template.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.template.model.po.MsgTemplate


/**
 * 消息模板业务接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgTemplateService : IBaseCrudService<String, MsgTemplate> {


    /**
     * 根据id获取模板缓存项。当前实现直接走 DAO，无独立缓存层；保留 CacheEntry 返回类型便于
     * 后续接入 hash-cache 而不破坏调用方。
     *
     * @param id 模板主键
     * @return MsgTemplateCacheEntry，找不到返回 null
     */
    fun getTemplateById(id: String): MsgTemplateCacheEntry?

    /**
     * 按租户 + 事件类型 + 消息类型 + 语言 查找模板。
     * 这四元组是发送端选模板的天然路由键（同一事件可针对不同消息类型/语言准备不同文案）。
     * 若 `localeDictCode` 传 null 则忽略 locale 维度，留给调用方退到默认语言。
     *
     * @return 第一条匹配的模板；找不到返回 null
     */
    fun getTemplateByEvent(
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry?


}
