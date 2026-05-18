package io.kudos.ms.sys.common.outline.api

import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 出网白名单 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysOutLineApi {

    /**
     * 返回指定系统下、指定租户的所有启用的出网白名单。
     * `tenantId == null` 表示平台级规则。
     *
     * @param systemCode 系统编码
     * @param tenantId 租户id；为 `null` 时查询平台级规则
     * @return 出网白名单列表（仅包含 active=true 的记录）
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/outLine/listOutLines")
    fun listOutLines(
        @RequestParam systemCode: String,
        @RequestParam(required = false) tenantId: String? = null
    ): List<SysOutLineCacheEntry>

}
