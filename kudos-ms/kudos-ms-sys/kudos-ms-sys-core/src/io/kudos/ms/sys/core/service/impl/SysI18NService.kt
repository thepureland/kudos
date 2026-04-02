package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.ms.sys.common.vo.i18n.SysI18nCacheEntry
import io.kudos.ms.sys.common.vo.i18n.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.cache.SysI18nHashCache
import io.kudos.ms.sys.core.dao.SysI18nDao
import io.kudos.ms.sys.core.model.po.SysI18n
import io.kudos.ms.sys.core.service.iservice.ISysI18nService
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
) : BaseCrudService<String, SysI18n, SysI18nDao>(dao), ISysI18nService {

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysI18nCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysI18nHashCache.getI18nById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getI18nFromCache(id: String): SysI18nCacheEntry? = sysI18nHashCache.getI18nById(id)

    override fun getI18nValueFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String? = getI18nsFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode)[key]

    override fun getI18nsFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String
    ): Map<String, String> {
        return sysI18nHashCache.getI18nMap(locale, atomicServiceCode, i18nTypeDictCode, namespace)
    }

    override fun batchGetI18nsFromCache(
        locale: String,
        namespacesByI18nTypeDictCode: Map<String, Collection<String>>,
        atomicServiceCodes: Collection<String>
    ): Map<String, Map<String, Map<String, String>>> {
        return namespacesByI18nTypeDictCode.mapValues { (i18nTypeDictCode, namespaces) ->
            namespaces.associateWith { namespace ->
                val map = mutableMapOf<String, String>()
                atomicServiceCodes.forEach { atomicServiceCode ->
                    map.putAll(
                        sysI18nHashCache.getI18nMap(locale, atomicServiceCode, i18nTypeDictCode, namespace)
                    )
                }
                map
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
            sysI18nHashCache.syncOnUpdate(id)
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的国际化内容。") {
            sysI18nHashCache.syncOnInsert(any, id)
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireI18nId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的国际化内容。",
            failureMessage = "更新id为${id}的国际化内容失败！",
        ) {
            sysI18nHashCache.syncOnUpdate(any, id)
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
            sysI18nHashCache.syncOnDelete(id)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除国际化内容，期望删除${ids.size}条，实际删除${count}条。")
        sysI18nHashCache.syncOnBatchDelete(ids)
        return count
    }

    private fun resolveNamespaceAndKey(payload: SysI18nFormUpdate): Pair<String, String> {
        val key = requireNotNull(payload.key) { "key不能为空。" }
        val i18nTypeDictCode = requireNotNull(payload.i18nTypeDictCode) { "i18nTypeDictCode不能为空。" }
        val namespace = payload.namespace.takeIf { it.isNotBlank() } ?: i18nTypeDictCode
        return namespace to key
    }

    private fun saveOrUpdateI18n(form: SysI18nFormUpdate): Boolean {
        return if (form.id.isNullOrBlank()) {
            val i18n = toI18n(form, creating = true)
            val id = dao.insert(i18n)
            sysI18nHashCache.syncOnInsert(i18n, id)
            true
        } else {
            val i18n = toI18n(form, creating = false)
            dao.update(i18n).also { updated ->
                if (updated) {
                    sysI18nHashCache.syncOnUpdate(i18n, i18n.id)
                }
            }
        }
    }

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

    private fun requireI18nId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新国际化内容时不支持的入参类型: ${any::class.qualifiedName}")
}
