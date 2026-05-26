package io.kudos.ms.msg.common.template.api

import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * Public API for message templates.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgTemplateApi {


    /**
     * Get a template by id.
     *
     * @param id template primary key
     * @return MsgTemplateCacheEntry, or null if not found
     */
    @GetMapping("/api/internal/msg/template/getTemplateById")
    fun getTemplateById(@RequestParam id: String): MsgTemplateCacheEntry?

    /**
     * Look up a template by (tenantId, eventType, msgType, locale).
     * `localeDictCode` is nullable: callers should try with a locale first, and fall back to null
     * for the default language when no match is found.
     */
    @GetMapping("/api/internal/msg/template/getTemplateByEvent")
    fun getTemplateByEvent(
        @RequestParam tenantId: String,
        @RequestParam eventTypeDictCode: String,
        @RequestParam msgTypeDictCode: String,
        @RequestParam(required = false) localeDictCode: String?,
    ): MsgTemplateCacheEntry?


}
