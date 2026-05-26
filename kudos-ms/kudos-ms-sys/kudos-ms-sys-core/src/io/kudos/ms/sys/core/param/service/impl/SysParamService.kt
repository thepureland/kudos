package io.kudos.ms.sys.core.param.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.platform.service.impl.requireStringId
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.common.param.vo.response.SysParamRow
import io.kudos.ms.sys.core.param.cache.ParamByModuleAndNameCache
import io.kudos.ms.sys.core.param.dao.SysParamDao
import io.kudos.ms.sys.core.param.event.SysParamBatchDeleted
import io.kudos.ms.sys.core.param.event.SysParamDeleted
import io.kudos.ms.sys.core.param.event.SysParamInserted
import io.kudos.ms.sys.core.param.event.SysParamUpdated
import io.kudos.ms.sys.core.param.model.po.SysParam
import io.kudos.ms.sys.core.param.service.iservice.ISysParamService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * Parameter business service.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysParamService(
    dao: SysParamDao,
    private val paramByModuleAndNameCache: ParamByModuleAndNameCache,
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysParam, SysParamDao>(dao), ISysParamService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysParamCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            dao.get(id, SysParamCacheEntry::class) as R?
        } else {
            super.get(id, returnType)
        }

    @Transactional(readOnly = true)
    override fun getParamFromCache(atomicServiceCode: String, paramName: String): SysParamCacheEntry? =
        paramByModuleAndNameCache.getParam(atomicServiceCode, paramName)

    @Transactional(readOnly = true)
    override fun getParamsByAtomicServiceCode(atomicServiceCode: String): List<SysParamRow> =
        dao.searchAs(Criteria(SysParam::atomicServiceCode eq atomicServiceCode))

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val param = SysParam {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(param),
            log = log,
            successMessage = "Updated active state of parameter with id ${id} to ${active}.",
            failureMessage = "Failed to update active state of parameter with id ${id} to ${active}!",
        ) {
            eventPublisher.publishEvent(SysParamUpdated(id = id))
        }
    }

    @Transactional(readOnly = true)
    override fun getParamValueFromCache(atomicServiceCode: String, paramName: String, defaultValue: String?): String? {
        val param = getParamFromCache(atomicServiceCode, paramName)
        return param?.paramValue ?: param?.defaultValue ?: defaultValue
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted parameter with id ${id}.") {
            eventPublisher.publishEvent(SysParamInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireStringId(any, "parameter")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated parameter with id ${id}.",
            failureMessage = "Failed to update parameter with id ${id}!",
        ) {
            eventPublisher.publishEvent(SysParamUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val param = dao.get(id) ?: run {
            log.warn("Parameter with id ${id} no longer exists when attempting deletion!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted parameter with id ${id}.",
            failureMessage = "Failed to delete parameter with id ${id}!",
        ) {
            eventPublisher.publishEvent(
                SysParamDeleted(id = id, atomicServiceCode = param.atomicServiceCode, paramName = param.paramName)
            )
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val params = dao.inSearchById(ids)
        val moduleAndNames = params.map { Pair(it.atomicServiceCode, it.paramName) }
        val count = super.batchDelete(ids)
        log.debug("Batch deleting parameters: expected ${ids.size}, actually deleted ${count}.")
        if (count > 0) {
            eventPublisher.publishEvent(SysParamBatchDeleted(ids = ids, moduleAndNames = moduleAndNames))
        }
        return count
    }

}
