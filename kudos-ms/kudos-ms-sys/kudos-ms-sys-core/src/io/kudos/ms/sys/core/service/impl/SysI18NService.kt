package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.i18n.SysI18nCacheEntry
import io.kudos.ms.sys.common.vo.i18n.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.cache.SysI18nHashCache
import io.kudos.ms.sys.core.dao.SysI18nDao
import io.kudos.ms.sys.core.model.po.SysI18n
import io.kudos.ms.sys.core.service.iservice.ISysI18nService
import org.springframework.beans.factory.annotation.Autowired
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
    dao: SysI18nDao
) : BaseCrudService<String, SysI18n, SysI18nDao>(dao), ISysI18nService {

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private lateinit var sysI18nHashCache: SysI18nHashCache

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysI18nCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysI18nHashCache.getI18nById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getI18nFromCache(id: String): SysI18nCacheEntry? {
        return sysI18nHashCache.getI18nById(id)
    }

    override fun getI18nValueFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String? {
        val i18nMap = getI18nsFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode)
        return i18nMap[key]
    }

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
        var count = 0
        i18ns.forEach { form ->
            if (form.id.isNullOrBlank()) {
                val locale = requireNotNull(form.locale) { "新增国际化内容时，locale不能为空。" }
                val atomicServiceCode =
                    requireNotNull(form.atomicServiceCode) { "新增国际化内容时，atomicServiceCode不能为空。" }
                val i18nTypeDictCode =
                    requireNotNull(form.i18nTypeDictCode) { "新增国际化内容时，i18nTypeDictCode不能为空。" }
                val (namespace, key) = resolveNamespaceAndKey(form)
                val value = requireNotNull(form.value) { "新增国际化内容时，value不能为空。" }
                val i18n = SysI18n {
                    this.locale = locale
                    this.atomicServiceCode = atomicServiceCode
                    this.i18nTypeDictCode = i18nTypeDictCode
                    this.namespace = namespace
                    this.key = key
                    this.value = value
                    this.remark = form.remark
                }
                val id = dao.insert(i18n)
                sysI18nHashCache.syncOnInsert(i18n, id)
                count++
            } else {
                val locale = requireNotNull(form.locale) { "更新国际化内容时，locale不能为空。" }
                val atomicServiceCode =
                    requireNotNull(form.atomicServiceCode) { "更新国际化内容时，atomicServiceCode不能为空。" }
                val i18nTypeDictCode =
                    requireNotNull(form.i18nTypeDictCode) { "更新国际化内容时，i18nTypeDictCode不能为空。" }
                val (namespace, key) = resolveNamespaceAndKey(form)
                val value = requireNotNull(form.value) { "更新国际化内容时，value不能为空。" }
                val i18n = SysI18n {
                    this.id = form.id!!
                    this.locale = locale
                    this.atomicServiceCode = atomicServiceCode
                    this.i18nTypeDictCode = i18nTypeDictCode
                    this.namespace = namespace
                    this.key = key
                    this.value = value
                    this.remark = form.remark
                }
                if (dao.update(i18n)) {
                    sysI18nHashCache.syncOnUpdate(i18n, i18n.id)
                    count++
                }
            }
        }
        log.debug("批量保存或更新国际化内容，期望处理${i18ns.size}条，实际处理${count}条。")
        return count
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val i18n = SysI18n {
            this.id = id
            this.active = active
        }
        val success = dao.update(i18n)
        if (success) {
            log.debug("更新id为${id}的国际化内容的启用状态为${active}。")
            sysI18nHashCache.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的国际化内容的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的国际化内容。")
        sysI18nHashCache.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysI18n::id.name) as String
        if (success) {
            log.debug("更新id为${id}的国际化内容。")
            sysI18nHashCache.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的国际化内容失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的国际化内容时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的国际化内容。")
            sysI18nHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的国际化内容失败！")
        }
        return success
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

}
