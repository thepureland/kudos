package io.kudos.ms.msg.api.admin.controller.send

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.msg.common.send.vo.request.MsgSendFormCreate
import io.kudos.ms.msg.common.send.vo.request.MsgSendFormUpdate
import io.kudos.ms.msg.common.send.vo.request.MsgSendQuery
import io.kudos.ms.msg.common.send.vo.response.MsgSendDetail
import io.kudos.ms.msg.common.send.vo.response.MsgSendEdit
import io.kudos.ms.msg.common.send.vo.response.MsgSendRow
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 消息发送管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/msg/send")
class MsgSendAdminController :
    BaseCrudController<String, IMsgSendService, MsgSendQuery, MsgSendRow, MsgSendDetail, MsgSendEdit, MsgSendFormCreate, MsgSendFormUpdate>()
