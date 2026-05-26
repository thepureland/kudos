package io.kudos.ms.sys.core.microservice.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.microservice.model.po.SysSubSystemMicroService
import io.kudos.ms.sys.core.microservice.model.table.SysSubSystemMicroServices
import org.springframework.stereotype.Repository


/**
 * Sub-system to micro-service relation DAO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class SysSubSystemMicroServiceDao : BaseCrudDao<String, SysSubSystemMicroService, SysSubSystemMicroServices>() {


    /**
     * Find micro-service codes by sub-system code.
     *
     * @param subSystemCode sub-system code
     * @return Set<micro-service code>
     */
    fun searchMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String> {
        val criteria = Criteria(SysSubSystemMicroService::subSystemCode eq subSystemCode)
        return searchProperty(criteria, SysSubSystemMicroService::microServiceCode).toSet()
    }

    /**
     * Find sub-system codes by micro-service code.
     *
     * @param microServiceCode micro-service code
     * @return Set<sub-system code>
     */
    fun fetchSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String> {
        val criteria = Criteria(SysSubSystemMicroService::microServiceCode eq microServiceCode)
        return searchProperty(criteria, SysSubSystemMicroService::subSystemCode).toSet()
    }

    /**
     * Check whether the relation exists.
     *
     * @param subSystemCode sub-system code
     * @param microServiceCode micro-service code
     * @return whether the relation exists
     */
    fun exists(subSystemCode: String, microServiceCode: String): Boolean {
        val criteria = Criteria.and(
            SysSubSystemMicroService::subSystemCode eq subSystemCode,
            SysSubSystemMicroService::microServiceCode eq microServiceCode
        )
        return count(criteria) > 0
    }

    /**
     * Delete the relation by sub-system code and micro-service code.
     *
     * @param subSystemCode sub-system code
     * @param microServiceCode micro-service code
     * @return number of rows deleted
     */
    fun deleteBySubSystemCodeAndMicroServiceCode(subSystemCode: String, microServiceCode: String): Int {
        val criteria = Criteria.and(
            SysSubSystemMicroService::subSystemCode eq subSystemCode,
            SysSubSystemMicroService::microServiceCode eq microServiceCode
        )
        return batchDeleteCriteria(criteria)
    }


}