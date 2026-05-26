package io.kudos.ms.user.api.admin.controller.login

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.common.login.vo.request.UserLoginRememberMeFormCreate
import io.kudos.ms.user.common.login.vo.request.UserLoginRememberMeFormUpdate
import io.kudos.ms.user.common.login.vo.request.UserLoginRememberMeQuery
import io.kudos.ms.user.common.login.vo.response.UserLoginRememberMeDetail
import io.kudos.ms.user.common.login.vo.response.UserLoginRememberMeEdit
import io.kudos.ms.user.common.login.vo.response.UserLoginRememberMeRow
import io.kudos.ms.user.core.login.service.iservice.IUserLoginRememberMeService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * Remember-me login admin controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/rememberMe")
class UserLoginRememberMeAdminController :
    BaseCrudController<String, IUserLoginRememberMeService, UserLoginRememberMeQuery, UserLoginRememberMeRow, UserLoginRememberMeDetail, UserLoginRememberMeEdit, UserLoginRememberMeFormCreate, UserLoginRememberMeFormUpdate>()
