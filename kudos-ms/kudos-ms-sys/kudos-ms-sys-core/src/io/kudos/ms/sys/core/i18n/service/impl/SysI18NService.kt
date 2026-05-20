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
 * 国际化业务
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
        log.debug("批量保存或更新国际化内容，期望处理${i18ns.size}条，实际处理${count}条。")
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
            successMessage = "更新id为${id}的国际化内容的启用状态为${active}。",
            failureMessage = "更新id为${id}的国际化内容的启用状态为${active}失败！",
        ) {
            eventPublisher.publishEvent(SysI18nUpdated(id = id))
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的国际化内容。") {
            eventPublisher.publishEvent(SysI18nInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "国际化内容")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的国际化内容。",
            failureMessage = "更新id为${id}的国际化内容失败！",
        ) {
            eventPublisher.publishEvent(SysI18nUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的国际化内容时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的国际化内容。",
            failureMessage = "删除id为${id}的国际化内容失败！",
        ) {
            eventPublisher.publishEvent(SysI18nDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除国际化内容，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysI18nBatchDeleted(ids = ids))
        }
        return count
    }

    /**
     * 标准化命名空间和 key：namespace 为空时退化为 `i18nTypeDictCode`（即把"类型"当默认命名空间）。
     *
     * @param payload 表单入参
     * @return `(namespace, key)` 对
     * @throws IllegalArgumentException key 或 i18nTypeDictCode 为空时
     * @author K
     * @since 1.0.0
     */
    private fun resolveNamespaceAndKey(payload: SysI18nFormUpdate): Pair<String, String> {
        val key = requireNotNull(payload.key) { "key不能为空。" }
        val i18nTypeDictCode = requireNotNull(payload.i18nTypeDictCode) { "i18nTypeDictCode不能为空。" }
        val namespace = payload.namespace.takeIf { it.isNotBlank() } ?: i18nTypeDictCode
        return namespace to key
    }

    /**
     * upsert 入口：根据 `form.id` 是否为空切换"insert + 发 Inserted 事件" 或 "update + 发 Updated 事件"。
     *
     * 注意：update 路径成功时才发事件，否则不发——避免下游基于事件做缓存失效却没真改库。
     *
     * @param form 入参（含可选 id）
     * @return 是否成功；insert 路径只要不抛异常即返回 true
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
     * 把 [SysI18nFormUpdate] 表单映射成 PO [SysI18n]：
     * - creating=false 时强制要求 id 非空（更新场景必须知道目标行）
     * - 其余字段统一校验非空，error message 区分"新增/更新"语境便于运维定位
     *
     * @param form 表单入参
     * @param creating true=新增场景，false=更新场景
     * @return PO 对象
     * @throws IllegalArgumentException 必填字段为空时
     * @author K
     * @since 1.0.0
     */
    private fun toI18n(form: SysI18nFormUpdate, creating: Boolean): SysI18n {
        val operation = if (creating) "新增" else "更新"
        val (namespace, key) = resolveNamespaceAndKey(form)
        return SysI18n {
            if (!creating) {
                this.id = requireNotNull(form.id) { "更新国际化内容时，id不能为空。" }
            }
            this.locale = requireNotNull(form.locale) { "${operation}国际化内容时，locale不能为空。" }
            this.atomicServiceCode = requireNotNull(form.atomicServiceCode) {
                "${operation}国际化内容时，atomicServiceCode不能为空。"
            }
            this.i18nTypeDictCode = requireNotNull(form.i18nTypeDictCode) {
                "${operation}国际化内容时，i18nTypeDictCode不能为空。"
            }
            this.namespace = namespace
            this.key = key
            this.value = requireNotNull(form.value) { "${operation}国际化内容时，value不能为空。" }
            this.remark = form.remark
        }
    }

}
