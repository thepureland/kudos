package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.i18n.SysI18nPayload
import io.kudos.ms.sys.common.vo.i18n.SysI18nRecord
import io.kudos.ms.sys.common.vo.i18n.SysI18nSearchPayload
import io.kudos.ms.sys.core.cache.I18NByLocaleAndTypeAndAmsCodeCache
import io.kudos.ms.sys.core.dao.SysI18nDao
import io.kudos.ms.sys.core.model.po.SysI18n
import io.kudos.ms.sys.core.service.iservice.ISysI18nService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 国际化业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysI18NService : BaseCrudService<String, SysI18n, SysI18nDao>(), ISysI18nService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var i18nCacheHandler: I18NByLocaleAndTypeAndAmsCodeCache

    override fun getI18nValue(locale: String, atomicServiceCode: String, i18nTypeDictCode: String, key: String): String? {
        val searchPayload = SysI18nSearchPayload().apply {
            this.locale = locale
            this.atomicServiceCode = atomicServiceCode
            this.i18nTypeDictCode = i18nTypeDictCode
            this.key = key
            this.active = true
        }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload, SysI18nRecord::class)
        return records.firstOrNull()?.value
    }

    override fun getI18nsByAtomicServiceAndType(atomicServiceCode: String, i18nTypeDictCode: String, locale: String?): List<SysI18nRecord> {
        val searchPayload = SysI18nSearchPayload().apply {
            this.atomicServiceCode = atomicServiceCode
            this.i18nTypeDictCode = i18nTypeDictCode
            this.locale = locale
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, SysI18nRecord::class)
    }

    @Transactional
    override fun batchSaveOrUpdate(i18ns: List<SysI18nPayload>): Int {
        var count = 0
        i18ns.forEach { payload ->
            if (payload.id.isNullOrBlank()) {
                val locale = requireNotNull(payload.locale) { "新增国际化内容时，locale不能为空。" }
                val atomicServiceCode = requireNotNull(payload.atomicServiceCode) { "新增国际化内容时，atomicServiceCode不能为空。" }
                val i18nTypeDictCode = requireNotNull(payload.i18nTypeDictCode) { "新增国际化内容时，i18nTypeDictCode不能为空。" }
                val key = requireNotNull(payload.key) { "新增国际化内容时，key不能为空。" }
                val value = requireNotNull(payload.value) { "新增国际化内容时，value不能为空。" }
                val i18n = SysI18n {
                    this.locale = locale
                    this.atomicServiceCode = atomicServiceCode
                    this.i18nTypeDictCode = i18nTypeDictCode
                    this.key = key
                    this.value = value
                    this.active = payload.active ?: true
                }
                val id = dao.insert(i18n)
                i18nCacheHandler.syncOnInsert(i18n, id)
                count++
            } else {
                val locale = requireNotNull(payload.locale) { "更新国际化内容时，locale不能为空。" }
                val atomicServiceCode = requireNotNull(payload.atomicServiceCode) { "更新国际化内容时，atomicServiceCode不能为空。" }
                val i18nTypeDictCode = requireNotNull(payload.i18nTypeDictCode) { "更新国际化内容时，i18nTypeDictCode不能为空。" }
                val key = requireNotNull(payload.key) { "更新国际化内容时，key不能为空。" }
                val value = requireNotNull(payload.value) { "更新国际化内容时，value不能为空。" }
                val i18n = SysI18n {
                    this.id = payload.id
                    this.locale = locale
                    this.atomicServiceCode = atomicServiceCode
                    this.i18nTypeDictCode = i18nTypeDictCode
                    this.key = key
                    this.value = value
                    this.active = payload.active ?: true
                }
                if (dao.update(i18n)) {
                    i18nCacheHandler.syncOnUpdate(i18n, i18n.id)
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
            i18nCacheHandler.syncOnUpdateActive(id, active)
        } else {
            log.error("更新id为${id}的国际化内容的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的国际化内容。")
        i18nCacheHandler.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysI18n::id.name) as String
        if (success) {
            log.debug("更新id为${id}的国际化内容。")
            i18nCacheHandler.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的国际化内容失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val i18n = dao.get(id)
        if (i18n == null) {
            log.warn("删除id为${id}的国际化内容时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的国际化内容。")
            i18nCacheHandler.syncOnDelete(i18n, id)
        } else {
            log.error("删除id为${id}的国际化内容失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val i18ns = dao.inSearchById(ids)
        val keys = i18ns.map { i18nCacheHandler.getKey(it.locale, it.i18nTypeDictCode, it.atomicServiceCode) }
            .distinct()
        val count = super.batchDelete(ids)
        log.debug("批量删除国际化内容，期望删除${ids.size}条，实际删除${count}条。")
        i18nCacheHandler.syncOnBatchDelete(ids, keys)
        return count
    }

    //endregion your codes 2

}
