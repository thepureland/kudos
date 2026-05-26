package io.kudos.ms.msg.api.admin.controller.receiver

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiverGroupFormCreate
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiverGroupFormUpdate
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiverGroupQuery
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupDetail
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupEdit
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiverGroupRow
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiverGroupService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * Message receiver group admin controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/msg/receiverGroup")
class MsgReceiverGroupAdminController :
    BaseCrudController<String, IMsgReceiverGroupService, MsgReceiverGroupQuery, MsgReceiverGroupRow, MsgReceiverGroupDetail, MsgReceiverGroupEdit, MsgReceiverGroupFormCreate, MsgReceiverGroupFormUpdate>()
