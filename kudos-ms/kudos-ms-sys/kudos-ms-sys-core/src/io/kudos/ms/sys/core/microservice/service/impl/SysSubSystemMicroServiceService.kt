package io.kudos.ms.sys.core.microservice.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.microservice.dao.SysSubSystemMicroServiceDao
import io.kudos.ms.sys.core.microservice.model.po.SysSubSystemMicroService
import io.kudos.ms.sys.core.microservice.service.iservice.ISysSubSystemMicroServiceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 子系统-微服务关系业务
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
        // 一次 SELECT 已存在的关系，差集对新增 ID 一次 batchInsert，把原 N+1 折叠到 2 次 SQL。
        val existing = dao.searchMicroServiceCodesBySubSystemCode(subSystemCode)
        val newMicroServiceCodes = microServiceCodes.toSet() - existing
        if (newMicroServiceCodes.isEmpty()) {
            log.debug("批量绑定子系统${subSystemCode}与${microServiceCodes.size}个微服务的关系，全部已存在，无新增。")
            return 0
        }
        val relations = newMicroServiceCodes.map {
            SysSubSystemMicroService {
                this.subSystemCode = subSystemCode
                this.microServiceCode = it
            }
        }
        dao.batchInsert(relations)
        log.debug("批量绑定子系统${subSystemCode}与${microServiceCodes.size}个微服务的关系，成功绑定${newMicroServiceCodes.size}条。")
        return newMicroServiceCodes.size
    }

    @Transactional
    override fun unbind(subSystemCode: String, microServiceCode: String): Boolean {
        val count = dao.deleteBySubSystemCodeAndMicroServiceCode(subSystemCode, microServiceCode)
        val success = count > 0
        if (success) {
            log.debug("解绑子系统${subSystemCode}与微服务${microServiceCode}的关系。")
        } else {
            log.warn("解绑子系统${subSystemCode}与微服务${microServiceCode}的关系失败，关系不存在。")
        }
        return success
    }

    @Transactional(readOnly = true)
    override fun exists(subSystemCode: String, microServiceCode: String): Boolean {
        return dao.exists(subSystemCode, microServiceCode)
    }


}