package io.kudos.ms.user.api.admin.controller.account

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.common.account.vo.request.UserAccountProtectionFormCreate
import io.kudos.ms.user.common.account.vo.request.UserAccountProtectionFormUpdate
import io.kudos.ms.user.common.account.vo.request.UserAccountProtectionQuery
import io.kudos.ms.user.common.account.vo.response.UserAccountProtectionDetail
import io.kudos.ms.user.common.account.vo.response.UserAccountProtectionEdit
import io.kudos.ms.user.common.account.vo.response.UserAccountProtectionRow
import io.kudos.ms.user.core.account.service.iservice.IUserAccountProtectionService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 用户账号保护管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/accountProtection")
class UserAccountProtectionAdminController :
    BaseCrudController<String, IUserAccountProtectionService, UserAccountProtectionQuery, UserAccountProtectionRow, UserAccountProtectionDetail, UserAccountProtectionEdit, UserAccountProtectionFormCreate, UserAccountProtectionFormUpdate>()
