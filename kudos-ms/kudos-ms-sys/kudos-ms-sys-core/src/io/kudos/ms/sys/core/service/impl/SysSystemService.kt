package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.base.tree.ListToTreeConverter
import io.kudos.ms.sys.common.vo.system.SysSystemCacheEntry
import io.kudos.ms.sys.common.vo.system.SysSystemRow
import io.kudos.ms.sys.core.cache.SysSystemHashCache
import io.kudos.ms.sys.core.dao.SysSystemDao
import io.kudos.ms.sys.core.model.po.SysSystem
import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 系统业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysSystemService : BaseCrudService<String, SysSystem, SysSystemDao>(), ISysSystemService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var sysSystemHashCache: SysSystemHashCache

    override fun getSystemByCode(code: String): SysSystemCacheEntry? {
        return sysSystemHashCache.getSystemByCode(code)
    }

    override fun getFullSystemTree(): List<IdAndNameTreeNode<String>> {
        val cacheEntries = sysSystemHashCache.getAllSystems()
        val nodes = cacheEntries.map { IdAndNameTreeNode(it.code, it.name, it.parentCode) }
        return ListToTreeConverter.convert(nodes)
    }

    override fun getAllActiveSystems(): List<SysSystemRow> {
        val criteria = Criteria(SysSystem::active eq true)
        return dao.searchAs<SysSystemRow>(criteria)
    }

    @Transactional
    override fun updateActive(code: String, active: Boolean): Boolean {
        val system = SysSystem {
            this.code = code
            this.active = active
        }
        val success = dao.update(system)
        if (success) {
            log.debug("更新编码为${code}的系统的启用状态为${active}。")
            sysSystemHashCache.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的系统的启用状态为${active}失败！")
        }
        return success
    }

    override fun getSubSystemsBySystemCode(systemCode: String): List<SysSystemRow> {
        val criteria = Criteria.and(
            SysSystem::active eq true,
            SysSystem::parentCode eq systemCode
        )
        return dao.search<SysSystemRow>(criteria)
    }

    @Transactional
    override fun insert(any: Any): String {
        val code = super.insert(any)
        log.debug("新增编码为${code}的系统。")
        sysSystemHashCache.syncOnInsert(code)
        return code
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val code = BeanKit.getProperty(any, SysSystem::code.name) as String
        if (success) {
            log.debug("更新编码为${code}的系统。")
            sysSystemHashCache.syncOnUpdate(code)
        } else {
            log.error("更新编码为${code}的系统失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除编码为${id}的系统。")
            sysSystemHashCache.syncOnDelete(id)
        } else {
            log.error("删除编码为${id}的系统失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除系统，期望删除${ids.size}条，实际删除${count}条。")
        sysSystemHashCache.syncOnBatchDelete(ids)
        return count
    }

    //endregion your codes 2

}