package io.kudos.ms.sys.core.outline.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.model.po.SysOutLine


/**
 * 出网白名单业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface ISysOutLineService : IBaseCrudService<String, SysOutLine> {

    /**
     * 列出指定系统/租户下启用的所有出网白名单。`tenantId == null` 表示平台级规则。
     *
     * @param systemCode 系统编码，非空
     * @param tenantId 租户id；为 `null` 时查询平台级规则
     * @return 已启用的出网白名单缓存项
     */
    fun listActiveOutLines(systemCode: String, tenantId: String? = null): List<SysOutLineCacheEntry>

    /**
     * 更新启用状态
     *
     * @param id 出网白名单 id
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
