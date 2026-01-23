package io.kudos.ams.sys.provider.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.provider.model.po.SysMicroServiceAtomicService


/**
 * 微服务-原子服务关系业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysMicroServiceAtomicServiceService : IBaseCrudService<String, SysMicroServiceAtomicService> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据微服务编码获取原子服务编码列表
     *
     * @param microServiceCode 微服务编码
     * @return 原子服务编码集合
     * @author K
     * @since 1.0.0
     */
    fun getAtomicServiceCodesByMicroServiceCode(microServiceCode: String): Set<String>

    /**
     * 根据原子服务编码获取微服务编码列表
     *
     * @param atomicServiceCode 原子服务编码
     * @return 微服务编码集合
     * @author K
     * @since 1.0.0
     */
    fun getMicroServiceCodesByAtomicServiceCode(atomicServiceCode: String): Set<String>

    /**
     * 批量绑定微服务与原子服务的关系
     *
     * @param microServiceCode 微服务编码
     * @param atomicServiceCodes 原子服务编码集合
     * @return 成功绑定的数量
     * @author K
     * @since 1.0.0
     */
    fun batchBind(microServiceCode: String, atomicServiceCodes: Collection<String>): Int

    /**
     * 解绑微服务与原子服务的关系
     *
     * @param microServiceCode 微服务编码
     * @param atomicServiceCode 原子服务编码
     * @return 是否解绑成功
     * @author K
     * @since 1.0.0
     */
    fun unbind(microServiceCode: String, atomicServiceCode: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param microServiceCode 微服务编码
     * @param atomicServiceCode 原子服务编码
     * @return 是否存在
     * @author K
     * @since 1.0.0
     */
    fun exists(microServiceCode: String, atomicServiceCode: String): Boolean

    //endregion your codes 2

}