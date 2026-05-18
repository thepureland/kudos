package io.kudos.ms.sys.core.locale.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.model.po.SysLocale


/**
 * 语言/区域字典业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface ISysLocaleService : IBaseCrudService<String, SysLocale> {

    /**
     * 按语言代码查询启用的语言。
     *
     * @param code 语言代码，非空
     * @return 已启用的语言；查无结果或未启用返回 `null`
     */
    fun getLocaleByCode(code: String): SysLocaleCacheEntry?

    /**
     * 列出所有启用的语言（按 sort_no 升序）。
     */
    fun listActiveLocales(): List<SysLocaleCacheEntry>

    /**
     * 更新启用状态
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
