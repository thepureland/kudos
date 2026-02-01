package io.kudos.ams.sys.core.service.impl

import io.kudos.ams.sys.core.service.iservice.ISysMicroServiceAtomicServiceService
import io.kudos.ams.sys.core.model.po.SysMicroServiceAtomicService
import io.kudos.ams.sys.core.dao.SysMicroServiceAtomicServiceDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.logger.LogFactory
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 微服务-原子服务关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysMicroServiceAtomicServiceService : BaseCrudService<String, SysMicroServiceAtomicService, SysMicroServiceAtomicServiceDao>(), ISysMicroServiceAtomicServiceService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getAtomicServiceCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        return dao.searchAtomicServiceCodesByMicroServiceCode(microServiceCode)
    }

    override fun getMicroServiceCodesByAtomicServiceCode(atomicServiceCode: String): Set<String> {
        return dao.searchMicroServiceCodesByAtomicServiceCode(atomicServiceCode)
    }

    @Transactional
    override fun batchBind(microServiceCode: String, atomicServiceCodes: Collection<String>): Int {
        if (atomicServiceCodes.isEmpty()) {
            return 0
        }
        var count = 0
        atomicServiceCodes.forEach { atomicServiceCode ->
            if (!exists(microServiceCode, atomicServiceCode)) {
                val relation = SysMicroServiceAtomicService {
                    this.microServiceCode = microServiceCode
                    this.atomicServiceCode = atomicServiceCode
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定微服务${microServiceCode}与${atomicServiceCodes.size}个原子服务的关系，成功绑定${count}条。")
        return count
    }

    @Transactional
    override fun unbind(microServiceCode: String, atomicServiceCode: String): Boolean {
        val criteria = Criteria.of(SysMicroServiceAtomicService::microServiceCode.name, OperatorEnum.EQ, microServiceCode)
            .addAnd(SysMicroServiceAtomicService::atomicServiceCode.name, OperatorEnum.EQ, atomicServiceCode)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑微服务${microServiceCode}与原子服务${atomicServiceCode}的关系。")
        } else {
            log.warn("解绑微服务${microServiceCode}与原子服务${atomicServiceCode}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(microServiceCode: String, atomicServiceCode: String): Boolean {
        return dao.exists(microServiceCode, atomicServiceCode)
    }

    //endregion your codes 2

}