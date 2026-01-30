package io.kudos.ams.user.provider.service.iservice

import io.kudos.ams.user.provider.model.po.UserOrgUser
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 机构-用户关系业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface IUserOrgUserService : IBaseCrudService<String, UserOrgUser> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据机构ID获取用户ID集合
     *
     * @param orgId 机构ID
     * @return 用户ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserIdsByOrgId(orgId: String): Set<String>

    /**
     * 根据用户ID获取机构ID集合
     *
     * @param userId 用户ID
     * @return 机构ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getOrgIdsByUserId(userId: String): Set<String>

    /**
     * 批量绑定机构和用户关系
     *
     * @param orgId 机构ID
     * @param userIds 用户ID集合
     * @param orgAdmin 是否为机构管理员，默认为false
     * @return 成功绑定的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(orgId: String, userIds: Collection<String>, orgAdmin: Boolean = false): Int

    /**
     * 解绑机构和用户关系
     *
     * @param orgId 机构ID
     * @param userId 用户ID
     * @return 是否解绑成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(orgId: String, userId: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param orgId 机构ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(orgId: String, userId: String): Boolean

    /**
     * 设置/取消机构管理员
     *
     * @param orgId 机构ID
     * @param userId 用户ID
     * @param isAdmin 是否为管理员
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun setOrgAdmin(orgId: String, userId: String, isAdmin: Boolean): Boolean

    //endregion your codes 2

}
