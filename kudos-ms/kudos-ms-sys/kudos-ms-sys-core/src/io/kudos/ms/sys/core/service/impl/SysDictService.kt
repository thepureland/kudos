package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.vo.dict.SysDictCacheEntry
import io.kudos.ms.sys.common.vo.dict.response.SysDictRow
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.cache.SysDictHashCache
import io.kudos.ms.sys.core.dao.SysDictDao
import io.kudos.ms.sys.core.model.po.SysDict
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import io.kudos.ms.sys.core.service.iservice.ISysDictService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.LinkedHashMap
import kotlin.reflect.KClass


/**
 * 字典业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysDictService(
    dao: SysDictDao
) : BaseCrudService<String, SysDict, SysDictDao>(dao), ISysDictService {

    @Autowired
    private lateinit var sysDictItemService: ISysDictItemService

    @Autowired
    private lateinit var sysDictHashCache: SysDictHashCache

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysDictCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysDictHashCache.getDictById(id) as R?
        } else {
            super.get(id, returnType)
        }
    }

    override fun getDictFromCache(dictId: String): SysDictCacheEntry? {
        return sysDictHashCache.getDictById(dictId)
    }

    override fun getRecord(id: String): SysDictRow? {
        val dict = dao.get(id) ?: return null
        return toSysDictRow(dict)
    }

    @Transactional
    override fun delete(id: String, isDict: Boolean): Boolean {
        return if (isDict) {
            dao.deleteDictItemsByDictId(id)
            val success = dao.deleteById(id)
            if (success) {
                sysDictHashCache.syncOnDelete(id)
            } else {
                log.error("删除id为${id}的字典失败！")
            }
            success
        } else {
            sysDictItemService.cascadeDeleteChildren(id)
        }
    }

    override fun getDictsFromCacheByAtomicServiceCode(
        atomicServiceCode: String,
        activeOnly: Boolean
    ): List<SysDictCacheEntry> {
        var dictTypes = sysDictHashCache.getDictsByAtomicServiceCode(atomicServiceCode)
        if (activeOnly) {
            dictTypes = dictTypes.filter { it.active }
        }
        return dictTypes
    }

    override fun getDictByAtomicServiceAndType(atomicServiceCode: String, dictType: String): SysDictRow? {
        val criteria = Criteria.and(
            SysDict::atomicServiceCode eq atomicServiceCode,
            SysDict::dictType eq dictType
        )
        val dict = dao.search(criteria).firstOrNull() ?: return null
        return toSysDictRow(dict)
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val dict = SysDict {
            this.id = id
            this.active = active
        }
        val success = dao.update(dict)
        if (success) {
            log.debug("更新id为${id}的字典的启用状态为${active}。")
            sysDictHashCache.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的字典的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的字典。")
        sysDictHashCache.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysDict::id.name) as String
        if (success) {
            log.debug("更新id为${id}的字典。")
            sysDictHashCache.syncOnUpdate(id)
        } else {
            log.error("更新id为${id}的字典失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的字典时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的字典。")
            sysDictHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的字典失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除字典，期望删除${ids.size}条，实际删除${count}条。")
        sysDictHashCache.syncOnBatchDelete(ids)
        return count
    }

    override fun getActiveDictItemsFromCache(
        dictType: String,
        atomicServiceCode: String
    ): List<SysDictItemCacheEntry> {
        return sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)
    }

    override fun getActiveDictItemMapFromCache(
        dictType: String,
        atomicServiceCode: String
    ): LinkedHashMap<String, String> {
        val items = sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)
        return LinkedHashMap<String, String>().apply {
            items.forEach { put(it.itemCode, it.itemName) }
        }
    }

    override fun batchGetActiveDictItemsFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> {
        return dictTypeAndASCodePairs.associate { (dictType, atomicServiceCode) ->
            Pair(atomicServiceCode, dictType) to sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)
        }
    }

    override fun batchGetActiveDictItemMapFromCache(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> {
        return dictTypeAndASCodePairs.associate { (dictType, atomicServiceCode) ->
            val items = sysDictItemService.getDictItemsFromCache(dictType, atomicServiceCode)
            val map = LinkedHashMap<String, String>().apply {
                items.forEach { put(it.itemCode, it.itemName) }
            }
            Pair(atomicServiceCode, dictType) to map
        }
    }

    private fun toSysDictRow(dict: SysDict): SysDictRow {
        return SysDictRow(
            id = dict.id,
            dictType = dict.dictType,
            dictName = dict.dictName,
            atomicServiceCode = dict.atomicServiceCode,
            remark = dict.remark,
            active = dict.active,
            builtIn = dict.builtIn,
        )
    }

}
