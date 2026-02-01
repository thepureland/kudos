package io.kudos.ams.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.core.model.po.SysSubSystemMicroService


/**
 * 子系统-微服务关系业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysSubSystemMicroServiceService : IBaseCrudService<String, SysSubSystemMicroService> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据子系统编码获取微服务编码列表
     *
     * @param subSystemCode 子系统编码
     * @return 微服务编码集合
     * @author K
     * @since 1.0.0
     */
    fun getMicroServiceCodesBySubSystemCode(subSystemCode: String): Set<String>

    /**
     * 根据微服务编码获取子系统编码列表
     *
     * @param microServiceCode 微服务编码
     * @return 子系统编码集合
     * @author K
     * @since 1.0.0
     */
    fun getSubSystemCodesByMicroServiceCode(microServiceCode: String): Set<String>

    /**
     * 批量绑定子系统与微服务的关系
     *
     * @param subSystemCode 子系统编码
     * @param microServiceCodes 微服务编码集合
     * @return 成功绑定的数量
     * @author K
     * @since 1.0.0
     */
    fun batchBind(subSystemCode: String, microServiceCodes: Collection<String>): Int

    /**
     * 解绑子系统与微服务的关系
     *
     * @param subSystemCode 子系统编码
     * @param microServiceCode 微服务编码
     * @return 是否解绑成功
     * @author K
     * @since 1.0.0
     */
    fun unbind(subSystemCode: String, microServiceCode: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param subSystemCode 子系统编码
     * @param microServiceCode 微服务编码
     * @return 是否存在
     * @author K
     * @since 1.0.0
     */
    fun exists(subSystemCode: String, microServiceCode: String): Boolean

    //endregion your codes 2

}