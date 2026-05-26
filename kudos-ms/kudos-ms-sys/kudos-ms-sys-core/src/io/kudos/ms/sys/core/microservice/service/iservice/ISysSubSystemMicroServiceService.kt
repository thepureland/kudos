package io.kudos.ms.sys.core.microservice.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.core.microservice.model.po.SysSubSystemMicroService


/**
 * Service interface for sub-system to microservice relationships.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysSubSystemMicroServiceService : IBaseCrudService<String, SysSubSystemMicroService> {


    /**
     * Get microservice codes by sub-system code.
     *
     * @param subSystemCode Sub-system code
     * @return Set of microservice codes
     * @author K
     * @since 1.0.0
     */
    fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String>

    /**
     * Get sub-system codes by microservice code.
     *
     * @param microServiceCode Microservice code
     * @return Set of sub-system codes
     * @author K
     * @since 1.0.0
     */
    fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String>

    /**
     * Batch bind sub-system to microservices.
     *
     * @param subSystemCode Sub-system code
     * @param microServiceCodes Collection of microservice codes
     * @return Number of bindings successfully created
     * @author K
     * @since 1.0.0
     */
    fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int

    /**
     * Unbind a sub-system from a microservice.
     *
     * @param subSystemCode Sub-system code
     * @param microServiceCode Microservice code
     * @return Whether unbinding succeeded
     * @author K
     * @since 1.0.0
     */
    fun unbind(subSystemCode: String, microServiceCode: String): Boolean

    /**
     * Check whether the relationship exists.
     *
     * @param subSystemCode Sub-system code
     * @param microServiceCode Microservice code
     * @return Whether the relationship exists
     * @author K
     * @since 1.0.0
     */
    fun exists(subSystemCode: String, microServiceCode: String): Boolean


}