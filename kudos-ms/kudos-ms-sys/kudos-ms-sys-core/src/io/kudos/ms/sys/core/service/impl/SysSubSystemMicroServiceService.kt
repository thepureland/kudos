package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.core.dao.SysSubSystemMicroServiceDao
import io.kudos.ms.sys.core.model.po.SysSubSystemMicroService
import io.kudos.ms.sys.core.service.iservice.ISysSubSystemMicroServiceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 子系统-微服务关系业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysSubSystemMicroServiceService : BaseCrudService<String, SysSubSystemMicroService, SysSubSystemMicroServiceDao>(), ISysSubSystemMicroServiceService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> {
        return dao.searchMicroServiceCodesBySubSystemCode(subSystemCode)
    }

    override fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        return dao.fetchSubSystemCodesByMicroServiceCode(microServiceCode)
    }

    @Transactional
    override fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int {
        if (microServiceCodes.isEmpty()) {
            return 0
        }
        var count = 0
        microServiceCodes.forEach { microServiceCode ->
            if (!exists(subSystemCode, microServiceCode)) {
                val relation = SysSubSystemMicroService {
                    this.subSystemCode = subSystemCode
                    this.microServiceCode = microServiceCode
                }
                dao.insert(relation)
                count++
            }
        }
        log.debug("批量绑定子系统${subSystemCode}与${microServiceCodes.size}个微服务的关系，成功绑定${count}条。")
        return count
    }

    @Transactional
    override fun unbind(subSystemCode: String, microServiceCode: String): Boolean {
        val criteria = Criteria.of(SysSubSystemMicroService::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
            .addAnd(SysSubSystemMicroService::microServiceCode.name, OperatorEnum.EQ, microServiceCode)
        val count = dao.batchDeleteCriteria(criteria)
        val success = count > 0
        if (success) {
            log.debug("解绑子系统${subSystemCode}与微服务${microServiceCode}的关系。")
        } else {
            log.warn("解绑子系统${subSystemCode}与微服务${microServiceCode}的关系失败，关系不存在。")
        }
        return success
    }

    override fun exists(subSystemCode: String, microServiceCode: String): Boolean {
        return dao.exists(subSystemCode, microServiceCode)
    }

    //endregion your codes 2

}