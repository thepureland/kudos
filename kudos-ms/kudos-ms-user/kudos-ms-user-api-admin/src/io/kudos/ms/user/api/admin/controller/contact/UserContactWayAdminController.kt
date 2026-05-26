package io.kudos.ms.user.api.admin.controller.contact

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.user.common.contact.vo.request.UserContactWayFormCreate
import io.kudos.ms.user.common.contact.vo.request.UserContactWayFormUpdate
import io.kudos.ms.user.common.contact.vo.request.UserContactWayQuery
import io.kudos.ms.user.common.contact.vo.response.UserContactWayDetail
import io.kudos.ms.user.common.contact.vo.response.UserContactWayEdit
import io.kudos.ms.user.common.contact.vo.response.UserContactWayRow
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * User contact way admin controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/user/contactWay")
class UserContactWayAdminController :
    BaseCrudController<String, IUserContactWayService, UserContactWayQuery, UserContactWayRow, UserContactWayDetail, UserContactWayEdit, UserContactWayFormCreate, UserContactWayFormUpdate>()
