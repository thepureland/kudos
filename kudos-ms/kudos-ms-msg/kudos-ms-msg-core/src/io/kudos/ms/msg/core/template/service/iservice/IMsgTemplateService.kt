package io.kudos.ms.msg.core.template.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.template.vo.MsgTemplateCacheEntry
import io.kudos.ms.msg.core.template.model.po.MsgTemplate


/**
 * Message template business service interface.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgTemplateService : IBaseCrudService<String, MsgTemplate> {


    /**
     * Gets the template cache entry by id. The current implementation goes directly to the DAO with
     * no separate cache layer; the CacheEntry return type is kept so a future hash-cache integration
     * will not break callers.
     *
     * @param id template primary key
     * @return MsgTemplateCacheEntry, or null if not found
     */
    fun getTemplateById(id: String): MsgTemplateCacheEntry?

    /**
     * Looks up a template by tenant + event type + message type + language.
     * This 4-tuple is the natural routing key for the sender to select a template (the same event can
     * have different content prepared for different message types/languages).
     * If `localeDictCode` is null the locale dimension is ignored, leaving the caller to fall back to the default language.
     *
     * @return the first matching template; null if not found
     */
    fun getTemplateByEvent(
        tenantId: String,
        eventTypeDictCode: String,
        msgTypeDictCode: String,
        localeDictCode: String?,
    ): MsgTemplateCacheEntry?


}
