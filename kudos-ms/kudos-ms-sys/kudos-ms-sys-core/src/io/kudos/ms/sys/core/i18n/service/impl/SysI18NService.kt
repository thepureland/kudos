package io.kudos.ms.sys.core.i18n.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.cache.SysI18nHashCache
import io.kudos.ms.sys.core.i18n.dao.SysI18nDao
import io.kudos.ms.sys.core.i18n.event.SysI18nBatchDeleted
import io.kudos.ms.sys.core.i18n.event.SysI18nDeleted
import io.kudos.ms.sys.core.i18n.event.SysI18nInserted
import io.kudos.ms.sys.core.i18n.event.SysI18nUpdated
import io.kudos.ms.sys.core.i18n.model.po.SysI18n
import io.kudos.ms.sys.core.i18n.service.iservice.ISysI18nService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * Business service for i18n entries.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysI18NService(
    dao: SysI18nDao,
    private val sysI18nHashCache: SysI18nHashCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysI18n, SysI18nDao>(dao), ISysI18nService {

    private val log = LogFactory.getLog(this::class)

    @Suppress("UNCHECKED_CAST")
    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysI18nCacheEntry::class) sysI18nHashCache.getI18nById(id) as R?
        else super.get(id, returnType)

    @Transactional(readOnly = true)
    override fun getI18nFromCache(id: String): SysI18nCacheEntry? = sysI18nHashCache.getI18nById(id)

    @Transactional(readOnly = true)
    override fun getI18nValueFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String? = getI18nsFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode)[key]

    @Transactional(readOnly = true)
    override fun getI18nsFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String
    ): Map<String, String> = sysI18nHashCache.getI18nMap(locale, atomicServiceCode, i18nTypeDictCode, namespace)

    @Transactional(readOnly = true)
    override fun batchGetI18nsFromCache(
        locale: String,
        namespacesByI18nTypeDictCode: Map<String, Collection<String>>,
        atomicServiceCodes: Collection<String>
    ): Map<String, Map<String, Map<String, String>>> =
        namespacesByI18nTypeDictCode.mapValues { (i18nTypeDictCode, namespaces) ->
            namespaces.associateWith { namespace ->
                atomicServiceCodes.fold(mutableMapOf<String, String>()) { acc, atomicServiceCode ->
                    acc.apply { putAll(sysI18nHashCache.getI18nMap(locale, atomicServiceCode, i18nTypeDictCode, namespace)) }
                }
            }
        }

    @Transactional
    override fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int {
        val count = i18ns.count(::saveOrUpdateI18n)
        log.debug("Batch save or update of i18n entries: expected ${i18ns.size}, actually processed $count.")
        return count
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val i18n = SysI18n {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(i18n),
            log = log,
            successMessage = "Updated active status of i18n entry with id $id to $active.",
            failureMessage = "Failed to update active status of i18n entry with id $id to $active!",
        ) {
            eventPublisher.publishEvent(SysI18nUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted i18n entry with id $id.") {
            eventPublisher.publishEvent(SysI18nInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "i18n entry")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated i18n entry with id $id.",
            failureMessage = "Failed to update i18n entry with id $id!",
        ) {
            eventPublisher.publishEvent(SysI18nUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("Tried to delete i18n entry with id $id, but it no longer exists!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted i18n entry with id $id.",
            failureMessage = "Failed to delete i18n entry with id $id!",
        ) {
            eventPublisher.publishEvent(SysI18nDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("Batch delete of i18n entries: expected ${ids.size}, actually deleted $count.")
        if (count > 0) {
            eventPublisher.publishEvent(SysI18nBatchDeleted(ids = ids))
        }
        return count
    }

    /**
     * Normalize namespace and key: when namespace is blank, fall back to `i18nTypeDictCode` (i.e. use the "type" as the default namespace).
     *
     * @param payload form input
     * @return `(namespace, key)` pair
     * @throws IllegalArgumentException when key or i18nTypeDictCode is null
     * @author K
     * @since 1.0.0
     */
    private fun resolveNamespaceAndKey(payload: SysI18nFormUpdate): Pair<String, String> {
        val key = requireNotNull(payload.key) { "key must not be null." }
        val i18nTypeDictCode = requireNotNull(payload.i18nTypeDictCode) { "i18nTypeDictCode must not be null." }
        val namespace = payload.namespace.takeIf { it.isNotBlank() } ?: i18nTypeDictCode
        return namespace to key
    }

    /**
     * Upsert entry point: based on whether `form.id` is blank, switches between "insert + publish Inserted event" and "update + publish Updated event".
     *
     * Note: the update path publishes the event only on success, to avoid downstream cache invalidations triggered by events that did not actually change the database.
     *
     * @param form input (with optional id)
     * @return whether the operation succeeded; the insert path returns true as long as no exception is thrown
     * @author K
     * @since 1.0.0
     */
    private fun saveOrUpdateI18n(form: SysI18nFormUpdate): Boolean =
        if (form.id.isNullOrBlank()) {
            val id = dao.insert(toI18n(form, creating = true))
            eventPublisher.publishEvent(SysI18nInserted(id = id))
            true
        } else {
            val i18n = toI18n(form, creating = false)
            dao.update(i18n).also { if (it) eventPublisher.publishEvent(SysI18nUpdated(id = i18n.id)) }
        }

    /**
     * Map [SysI18nFormUpdate] to PO [SysI18n]:
     * - when creating=false, id must be non-null (update needs to know the target row)
     * - all other fields are uniformly validated to be non-null; error messages distinguish "insert/update" context to help with troubleshooting
     *
     * @param form form input
     * @param creating true=insert case, false=update case
     * @return PO object
     * @throws IllegalArgumentException when a required field is null
     * @author K
     * @since 1.0.0
     */
    private fun toI18n(form: SysI18nFormUpdate, creating: Boolean): SysI18n {
        val operation = if (creating) "insert" else "update"
        val (namespace, key) = resolveNamespaceAndKey(form)
        return SysI18n {
            if (!creating) {
                this.id = requireNotNull(form.id) { "id must not be null when updating an i18n entry." }
            }
            this.locale = requireNotNull(form.locale) { "locale must not be null when $operation of an i18n entry." }
            this.atomicServiceCode = requireNotNull(form.atomicServiceCode) {
                "atomicServiceCode must not be null when $operation of an i18n entry."
            }
            this.i18nTypeDictCode = requireNotNull(form.i18nTypeDictCode) {
                "i18nTypeDictCode must not be null when $operation of an i18n entry."
            }
            this.namespace = namespace
            this.key = key
            this.value = requireNotNull(form.value) { "value must not be null when $operation of an i18n entry." }
            this.remark = form.remark
        }
    }

}
