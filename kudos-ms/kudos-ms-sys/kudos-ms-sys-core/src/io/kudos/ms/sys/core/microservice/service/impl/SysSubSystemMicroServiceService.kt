package io.kudos.ms.sys.core.microservice.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.microservice.dao.SysSubSystemMicroServiceDao
import io.kudos.ms.sys.core.microservice.model.po.SysSubSystemMicroService
import io.kudos.ms.sys.core.microservice.service.iservice.ISysSubSystemMicroServiceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * Sub-system to microservice relationship service.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysSubSystemMicroServiceService(
    dao: SysSubSystemMicroServiceDao
) : BaseCrudService<String, SysSubSystemMicroService, SysSubSystemMicroServiceDao>(dao), ISysSubSystemMicroServiceService {


    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> {
        return dao.searchMicroServiceCodesBySubSystemCode(subSystemCode)
    }

    @Transactional(readOnly = true)
    override fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        return dao.fetchSubSystemCodesByMicroServiceCode(microServiceCode)
    }

    @Transactional
    override fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int {
        if (microServiceCodes.isEmpty()) {
            return 0
        }
        // One SELECT for existing relationships, then a single batchInsert on the diff; collapses the original N+1 to 2 SQL statements.
        val existing = dao.searchMicroServiceCodesBySubSystemCode(subSystemCode)
        val newMicroServiceCodes = microServiceCodes.toSet() - existing
        if (newMicroServiceCodes.isEmpty()) {
            log.debug("Batch bind for sub-system ${subSystemCode} with ${microServiceCodes.size} microservices: all already exist, nothing to insert.")
            return 0
        }
        val relations = newMicroServiceCodes.map {
            SysSubSystemMicroService {
                this.subSystemCode = subSystemCode
                this.microServiceCode = it
            }
        }
        dao.batchInsert(relations)
        log.debug("Batch bind for sub-system ${subSystemCode} with ${microServiceCodes.size} microservices: successfully bound ${newMicroServiceCodes.size} relationships.")
        return newMicroServiceCodes.size
    }

    @Transactional
    override fun unbind(subSystemCode: String, microServiceCode: String): Boolean {
        val count = dao.deleteBySubSystemCodeAndMicroServiceCode(subSystemCode, microServiceCode)
        val success = count > 0
        if (success) {
            log.debug("Unbound sub-system ${subSystemCode} from microservice ${microServiceCode}.")
        } else {
            log.warn("Failed to unbind sub-system ${subSystemCode} from microservice ${microServiceCode}: relationship does not exist.")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(subSystemCode: String, microServiceCode: String): Boolean {
        return dao.exists(subSystemCode, microServiceCode)
    }


}