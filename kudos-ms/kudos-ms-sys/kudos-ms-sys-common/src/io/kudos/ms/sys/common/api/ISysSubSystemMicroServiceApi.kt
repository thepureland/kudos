package io.kudos.ms.sys.common.api


/**
 * 子系统-微服务关系 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysSubSystemMicroServiceApi {


    fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String>

    fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String>

    fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int

    fun unbind(subSystemCode: String, microServiceCode: String): Boolean

    fun exists(subSystemCode: String, microServiceCode: String): Boolean


}