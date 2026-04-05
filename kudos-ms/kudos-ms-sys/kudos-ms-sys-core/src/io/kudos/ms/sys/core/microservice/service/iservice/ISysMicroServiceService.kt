package io.kudos.ms.sys.core.microservice.service.iservice
import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.model.po.SysMicroService


/**
 * 微服务业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysMicroServiceService : IBaseCrudService<String, SysMicroService> {

    /**
     * 按微服务编码加载微服务信息，并缓存结果
     *
     * @param code 微服务编码（主键），非空
     * @return 缓存项，找不到返回 null
     */
    fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry?

    /**
     * 从缓存获取全部微服务（含原子服务、含未启用）
     *
     * @return 微服务缓存项列表
     */
    fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry>

    /**
     * 从缓存获取非原子微服务列表，即 `atomicService == false`（含未启用）
     *
     * @return 微服务缓存项列表
     */
    fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry>

    /**
     * 从缓存获取全部原子微服务（`atomicService == true`，含未启用）
     *
     * @return 微服务缓存项列表
     */
    fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry>

    /**
     * 从缓存获取指定父编码下的微服务列表（按 parentCode 匹配，含未启用）
     *
     * @param parentCode 父微服务编码
     * @return 子级微服务缓存项列表
     */
    fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry>

    /**
     * 从缓存获取指定父编码下的原子微服务列表（`parentCode` 匹配且 `atomicService == true`，含未启用）
     *
     * @param parentCode 父微服务编码
     * @return 原子微服务缓存项列表
     */
    fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry>

    /**
     * 返回整棵微服务树（含层级关系）。
     *
     * @return 微服务树节点列表（根节点及其 children）
     */
    fun getFullMicroServiceTree(): List<IdAndNameTreeNode<String>>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 微服务编码（主键）
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(code: String, active: Boolean): Boolean


}
