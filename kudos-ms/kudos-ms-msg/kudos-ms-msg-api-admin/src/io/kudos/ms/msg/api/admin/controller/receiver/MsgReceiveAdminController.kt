package io.kudos.ms.msg.api.admin.controller.receiver

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiveFormCreate
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiveFormUpdate
import io.kudos.ms.msg.common.receiver.vo.request.MsgReceiveQuery
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveDetail
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveEdit
import io.kudos.ms.msg.common.receiver.vo.response.MsgReceiveRow
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 消息接收记录管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/msg/receive")
class MsgReceiveAdminController :
    BaseCrudController<String, IMsgReceiveService, MsgReceiveQuery, MsgReceiveRow, MsgReceiveDetail, MsgReceiveEdit, MsgReceiveFormCreate, MsgReceiveFormUpdate>()
