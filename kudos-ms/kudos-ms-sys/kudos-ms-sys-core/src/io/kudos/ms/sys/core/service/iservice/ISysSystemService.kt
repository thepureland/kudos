package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.vo.system.SysSystemCacheEntry
import io.kudos.ms.sys.core.model.po.SysSystem


/**
 * 系统业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysSystemService : IBaseCrudService<String, SysSystem> {

    /**
     * 按系统编码加载系统信息，并缓存结果
     *
     * @param code 系统编码（主键），非空
     * @return 缓存项，找不到返回 null
     */
    fun getSystemFromCache(code: String): SysSystemCacheEntry?

    /**
     * 从缓存获取全部系统（含未启用）
     *
     * @return 系统缓存项列表
     */
    fun getAllSystemsFromCache(): List<SysSystemCacheEntry>

    /**
     * 从缓存获取非子系统（顶级系统）列表，即 `subSystem == false`（含未启用）
     *
     * @return 系统缓存项列表
     */
    fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 系统编码（主键）
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(code: String, active: Boolean): Boolean

    /**
     * 从缓存获取指定父系统下的子系统列表（按 parentCode 匹配，含未启用）
     *
     * @param systemCode 父系统编码
     * @return 子系统缓存项列表
     */
    fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry>

    /**
     * 返回整棵系统树（含层级关系）。
     *
     * @return 系统树节点列表（根节点及其 children）
     */
    fun getFullSystemTree(): List<IdAndNameTreeNode<String>>


}
