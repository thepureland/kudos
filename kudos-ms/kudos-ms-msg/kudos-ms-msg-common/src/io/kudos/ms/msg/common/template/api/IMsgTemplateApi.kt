package io.kudos.ms.msg.common.template.api

import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 消息模板对外API
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgTemplateApi {


    /**
     * 根据id获取模板。
     *
     * @param id 模板主键
     * @return MsgTemplateCacheEntry，找不到返回 null
     */
    @GetMapping("/api/internal/msg/template/getTemplateById")
    fun getTemplateById(@RequestParam id: String): MsgTemplateCacheEntry?

    /**
     * 按 (tenantId, eventType, msgType, locale) 找模板。
     * `localeDictCode` 可空：调用方应先尝试携带 locale，找不到再以 null 兜底默认语言。
     */
    @GetMapping("/api/internal/msg/template/getTemplateByEvent")
    fun getTemplateByEvent(
        @RequestParam tenantId: String,
        @RequestParam eventTypeDictCode: String,
        @RequestParam msgTypeDictCode: String,
        @RequestParam(required = false) localeDictCode: String?,
    ): MsgTemplateCacheEntry?


}
