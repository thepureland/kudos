package io.kudos.ms.user.api.admin.controller.account

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.common.account.vo.request.UserAccountFormCreate
import io.kudos.ms.user.common.account.vo.request.UserAccountFormUpdate
import io.kudos.ms.user.common.account.vo.request.UserAccountQuery
import io.kudos.ms.user.common.account.vo.response.UserAccountDetail
import io.kudos.ms.user.common.account.vo.response.UserAccountEdit
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * 用户账号管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/account")
class UserAccountAdminController :
    BaseCrudController<String, IUserAccountService, UserAccountQuery, UserAccountRow, UserAccountDetail, UserAccountEdit, UserAccountFormCreate, UserAccountFormUpdate>() {

    /** 更新启用状态 */
    @PutMapping("/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean =
        service.updateActive(id, active)

    /** 重置登录密码（管理员操作） */
    @PostMapping("/resetPassword")
    fun resetPassword(@RequestParam id: String, @RequestParam newPassword: String): Boolean =
        service.resetPassword(id, newPassword)

    /** 重置安全密码（管理员操作） */
    @PostMapping("/resetSecurityPassword")
    fun resetSecurityPassword(@RequestParam id: String, @RequestParam newPassword: String): Boolean =
        service.resetSecurityPassword(id, newPassword)

}
