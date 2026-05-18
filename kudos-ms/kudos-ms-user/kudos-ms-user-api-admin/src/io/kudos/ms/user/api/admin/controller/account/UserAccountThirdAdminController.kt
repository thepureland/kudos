package io.kudos.ms.user.api.admin.controller.account

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.common.account.vo.request.UserAccountThirdFormCreate
import io.kudos.ms.user.common.account.vo.request.UserAccountThirdFormUpdate
import io.kudos.ms.user.common.account.vo.request.UserAccountThirdQuery
import io.kudos.ms.user.common.account.vo.response.UserAccountThirdDetail
import io.kudos.ms.user.common.account.vo.response.UserAccountThirdEdit
import io.kudos.ms.user.common.account.vo.response.UserAccountThirdRow
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * 用户第三方账号管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/accountThird")
class UserAccountThirdAdminController :
    BaseCrudController<String, IUserAccountThirdService, UserAccountThirdQuery, UserAccountThirdRow, UserAccountThirdDetail, UserAccountThirdEdit, UserAccountThirdFormCreate, UserAccountThirdFormUpdate>() {

    /** 列出指定用户的所有第三方绑定 */
    @GetMapping("/listByUserId")
    fun listByUserId(@RequestParam userId: String): List<UserAccountThird> =
        service.getByUserAccountId(userId)

}
