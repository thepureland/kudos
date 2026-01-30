package io.kudos.ams.user.common.vo.loginremember

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 记住我登录查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserLoginRememberMeSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = UserLoginRememberMeRecord::class,

    /** 用户名 */
    var username: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(UserLoginRememberMeRecord::class)

    //endregion your codes 3

}
