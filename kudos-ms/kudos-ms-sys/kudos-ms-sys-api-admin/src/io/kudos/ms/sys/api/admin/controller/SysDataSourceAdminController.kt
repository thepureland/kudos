package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.datasource.request.SysDataSourceFormCreate
import io.kudos.ms.sys.common.vo.datasource.request.SysDataSourceFormUpdate
import io.kudos.ms.sys.common.vo.datasource.request.SysDataSourceQuery
import io.kudos.ms.sys.common.vo.datasource.response.SysDataSourceDetail
import io.kudos.ms.sys.common.vo.datasource.response.SysDataSourceEdit
import io.kudos.ms.sys.common.vo.datasource.response.SysDataSourceRow
import io.kudos.ms.sys.core.service.iservice.ISysDataSourceService
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 数据源管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/dataSource")
class SysDataSourceAdminController:
    BaseCrudController<String, ISysDataSourceService, SysDataSourceQuery, SysDataSourceRow, SysDataSourceDetail, SysDataSourceEdit, SysDataSourceFormCreate, SysDataSourceFormUpdate>() {

    /**
     * 更新active状态
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     */
    @PutMapping("/updateActive")
    fun updateActive(id: String, active: Boolean): Boolean {
        return service.updateActive(id, active)
    }

}
