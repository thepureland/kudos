package io.kudos.ams.auth.provider.user.service.iservice

import io.kudos.ams.auth.common.vo.dept.AuthDeptCacheItem
import io.kudos.ams.auth.common.vo.dept.AuthDeptTreeRecord
import io.kudos.ams.auth.common.vo.user.AuthUserCacheItem
import io.kudos.ams.auth.provider.user.model.po.AuthDept
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 部门业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IAuthDeptService : IBaseCrudService<String, AuthDept> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据部门ID获取该部门的所有管理员用户信息
     *
     * @param deptId 部门ID
     * @return List<AuthUserCacheItem> 部门管理员用户列表，如果没有管理员则返回空列表
     */
    fun getDeptAdmins(deptId: String): List<AuthUserCacheItem>

    /**
     * 根据部门ID获取该部门下的所有用户ID列表（包括管理员和普通用户）
     *
     * @param deptId 部门ID
     * @return List<String> 用户ID列表，如果部门不存在或没有用户则返回空列表
     */
    fun getDeptUserIds(deptId: String): List<String>

    /**
     * 根据部门ID获取该部门的所有直接子部门ID列表
     *
     * @param deptId 部门ID
     * @return List<String> 子部门ID列表，如果没有子部门则返回空列表
     */
    fun getChildDeptIds(deptId: String): List<String>

    /**
     * 根据部门ID获取该部门下的所有用户列表（包括管理员和普通用户）
     *
     * @param deptId 部门ID
     * @return List<AuthUserCacheItem> 用户列表，如果部门不存在或没有用户则返回空列表
     */
    fun getDeptUsers(deptId: String): List<AuthUserCacheItem>

    /**
     * 检查用户是否属于指定部门
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return true表示用户属于该部门，false表示不属于
     */
    fun isUserInDept(userId: String, deptId: String): Boolean

    /**
     * 根据部门ID获取该部门的所有直接子部门列表
     *
     * @param deptId 部门ID
     * @return List<AuthDeptCacheItem> 子部门列表，如果没有子部门则返回空列表
     */
    fun getChildDepts(deptId: String): List<AuthDeptCacheItem>

    /**
     * 根据部门ID获取该部门的父部门
     *
     * @param deptId 部门ID
     * @return AuthDeptCacheItem 父部门，如果没有父部门则返回null
     */
    fun getParentDept(deptId: String): AuthDeptCacheItem?

    /**
     * 根据ID获取部门记录（从缓存）
     *
     * @param id 部门ID
     * @return 部门缓存项，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDeptRecord(id: String): AuthDeptCacheItem?

    /**
     * 根据租户ID获取部门列表
     *
     * @param tenantId 租户ID
     * @return 部门缓存项列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDeptsByTenantId(tenantId: String): List<AuthDeptCacheItem>

    /**
     * 获取部门树形结构
     *
     * @param tenantId 租户ID
     * @param parentId 父部门ID，为null时返回顶级部门
     * @return 部门树节点列表（树形结构，包含children字段）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getDeptTree(tenantId: String, parentId: String? = null): List<AuthDeptTreeRecord>

    /**
     * 获取所有祖先部门ID列表（向上递归）
     *
     * @param deptId 部门ID
     * @return 祖先部门ID列表（从直接父部门到根部门）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getAllAncestorDeptIds(deptId: String): List<String>

    /**
     * 获取所有后代部门ID列表（向下递归）
     *
     * @param deptId 部门ID
     * @return 后代部门ID列表（包括所有子部门、孙部门等）
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getAllDescendantDeptIds(deptId: String): List<String>

    /**
     * 更新部门启用状态
     *
     * @param id 部门ID
     * @param active 是否启用
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 移动部门（调整父部门和排序号）
     *
     * @param id 部门ID
     * @param newParentId 新的父部门ID，为null表示移动到顶级
     * @param newSortNum 新的排序号
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun moveDept(id: String, newParentId: String?, newSortNum: Int?): Boolean

    //endregion your codes 2

}
