package io.kudos.ms.msg.api.admin.controller.template

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateFormCreate
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateFormUpdate
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateQuery
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateDetail
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateEdit
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateRow
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 消息模板管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/msg/template")
class MsgTemplateAdminController :
    BaseCrudController<String, IMsgTemplateService, MsgTemplateQuery, MsgTemplateRow, MsgTemplateDetail, MsgTemplateEdit, MsgTemplateFormCreate, MsgTemplateFormUpdate>()
