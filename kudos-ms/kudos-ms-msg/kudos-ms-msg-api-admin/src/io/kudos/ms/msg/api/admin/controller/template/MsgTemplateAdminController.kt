package io.kudos.ms.msg.api.admin.controller.template

import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateFormCreate
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateFormUpdate
import io.kudos.ms.msg.common.template.vo.request.MsgTemplateQuery
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateDetail
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateEdit
import io.kudos.ms.msg.common.template.vo.response.MsgTemplateRow
import io.kudos.ms.msg.core.template.service.iservice.IMsgTemplateService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * Message template admin controller.
 *
 * Inherits the standard CRUD endpoints from [BaseCrudController]. The create / update / delete
 * mutations are overridden solely to attach [WebAudit] — they delegate straight back to `super`, so
 * behavior is unchanged but each operation now emits an audit log (captured by
 * [io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect] when the deployment wires an
 * audit store). Read endpoints (`list` / `getDetail` / `getEdit` / validation rules) stay un-audited.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/msg/template")
class MsgTemplateAdminController :
    BaseCrudController<String, IMsgTemplateService, MsgTemplateQuery, MsgTemplateRow, MsgTemplateDetail, MsgTemplateEdit, MsgTemplateFormCreate, MsgTemplateFormUpdate>() {

    @WebAudit(opType = OperationTypeEnum.CREATE, moduleCode = MODULE_CODE, desc = "创建消息模板")
    @PostMapping("/save")
    override fun save(@RequestBody @Valid formCreateVo: MsgTemplateFormCreate): String =
        super.save(formCreateVo)

    @WebAudit(opType = OperationTypeEnum.UPDATE, moduleCode = MODULE_CODE, desc = "修改消息模板")
    @PutMapping("/update")
    override fun update(@RequestBody @Valid formUpdateVo: MsgTemplateFormUpdate) {
        super.update(formUpdateVo)
    }

    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "删除消息模板")
    @DeleteMapping("/delete")
    override fun delete(id: String): Boolean = super.delete(id)

    @WebAudit(opType = OperationTypeEnum.DELETE, moduleCode = MODULE_CODE, desc = "批量删除消息模板")
    @PostMapping("/batchDelete")
    override fun batchDelete(@RequestBody ids: List<String>): Boolean = super.batchDelete(ids)

    companion object {
        /** Audit-log module code for message-template admin operations. */
        private const val MODULE_CODE = "msg-template"
    }
}
