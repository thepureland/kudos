package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.vo.system.SysSystemCacheEntry
import io.kudos.ms.sys.common.vo.system.response.SysSystemRow
import io.kudos.ms.sys.core.model.po.SysSystem


/**
 * 系统业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface ISysSystemService : IBaseCrudService<String, SysSystem> {


    /**
     * 根据编码从缓存获取系统信息
     *
     * @param code 系统编码
     * @return SysSystemCacheEntry，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getSystemByCode(code: String): SysSystemCacheEntry?

    /**
     * 获取所有启用的系统
     *
     * @return 系统记录列表
     * @author K
     * @since 1.0.0
     */
    fun getAllActiveSystems(): List<SysSystemRow>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 系统编码
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(code: String, active: Boolean): Boolean

    /**
     * 获取系统下的子系统列表
     *
     * @param systemCode 系统编码
     * @return 子系统记录列表
     * @author K
     * @since 1.0.0
     */
    fun getSubSystemsBySystemCode(systemCode: String): List<SysSystemRow>

    /**
     * 返回整棵系统树（含层级关系）。
     *
     * @return 系统树节点列表（根节点及其 children）
     */
    fun getFullSystemTree(): List<IdAndNameTreeNode<String>>


}
