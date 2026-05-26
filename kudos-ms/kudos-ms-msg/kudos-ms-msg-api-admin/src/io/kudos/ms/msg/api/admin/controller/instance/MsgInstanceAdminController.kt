package io.kudos.ms.msg.api.admin.controller.instance

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.msg.common.instance.vo.request.MsgInstanceFormCreate
import io.kudos.ms.msg.common.instance.vo.request.MsgInstanceFormUpdate
import io.kudos.ms.msg.common.instance.vo.request.MsgInstanceQuery
import io.kudos.ms.msg.common.instance.vo.response.MsgInstanceDetail
import io.kudos.ms.msg.common.instance.vo.response.MsgInstanceEdit
import io.kudos.ms.msg.common.instance.vo.response.MsgInstanceRow
import io.kudos.ms.msg.core.instance.service.iservice.IMsgInstanceService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * Message instance admin controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/msg/instance")
class MsgInstanceAdminController :
    BaseCrudController<String, IMsgInstanceService, MsgInstanceQuery, MsgInstanceRow, MsgInstanceDetail, MsgInstanceEdit, MsgInstanceFormCreate, MsgInstanceFormUpdate>()
