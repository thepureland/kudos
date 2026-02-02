package io.kudos.ms.user.core.service.iservice

import io.kudos.ms.user.core.model.po.UserAccountThird
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 用户账号第三方绑定业务接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface IUserAccountThirdService : IBaseCrudService<String, UserAccountThird> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据用户账号ID查询绑定列表
     *
     * @param userId 用户账号ID
     * @return 绑定列表
     */
    fun getByUserAccountId(userId: String): List<UserAccountThird>

    /**
     * 按第三方身份信息查询绑定记录
     *
     * @param tenantId 租户ID
     * @param accountProviderDictCode 第三方平台代码
     * @param accountProviderIssuer 发行方/平台租户
     * @param subject 第三方用户唯一标识
     * @return 绑定记录，找不到返回null
     */
    fun getByProviderSubject(
        tenantId: String,
        accountProviderDictCode: String,
        accountProviderIssuer: String?,
        subject: String
    ): UserAccountThird?

    //endregion your codes 2

}