package io.kudos.ams.auth.core.service.iservice

import io.kudos.ams.auth.core.model.po.AuthGroupUser
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 组-用户关系业务接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface IAuthGroupUserService : IBaseCrudService<String, AuthGroupUser> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据组ID获取用户ID集合
     *
     * @param groupId 组ID
     * @return 用户ID集合
     * @author AI: Codex
     * @since 1.0.0
     */
    fun getUserIdsByGroupId(groupId: String): Set<String>

    /**
     * 根据用户ID获取组ID集合
     *
     * @param userId 用户ID
     * @return 组ID集合
     * @author AI: Codex
     * @since 1.0.0
     */
    fun getGroupIdsByUserId(userId: String): Set<String>

    /**
     * 批量绑定组和用户关系
     *
     * @param groupId 组ID
     * @param userIds 用户ID集合
     * @return 成功绑定的数量
     * @author AI: Codex
     * @since 1.0.0
     */
    fun batchBind(groupId: String, userIds: Collection<String>): Int

    /**
     * 解绑组和用户关系
     *
     * @param groupId 组ID
     * @param userId 用户ID
     * @return 是否解绑成功
     * @author AI: Codex
     * @since 1.0.0
     */
    fun unbind(groupId: String, userId: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param groupId 组ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Codex
     * @since 1.0.0
     */
    fun exists(groupId: String, userId: String): Boolean

    //endregion your codes 2

}
