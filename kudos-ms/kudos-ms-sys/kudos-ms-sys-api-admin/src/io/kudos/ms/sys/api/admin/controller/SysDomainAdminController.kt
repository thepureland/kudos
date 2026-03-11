package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.domain.SysDomainDetail
import io.kudos.ms.sys.common.vo.domain.SysDomainForm
import io.kudos.ms.sys.common.vo.domain.SysDomainQuery
import io.kudos.ms.sys.common.vo.domain.SysDomainRow
import io.kudos.ms.sys.core.service.iservice.ISysDomainService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 域名管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/domain")
class SysDomainAdminController:
    BaseCrudController<String, ISysDomainService, SysDomainQuery, SysDomainRow, SysDomainDetail, SysDomainForm>() {



}