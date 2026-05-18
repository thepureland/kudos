package io.kudos.ms.sys.common.locale.api

import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 语言/区域字典 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysLocaleApi {

    /**
     * 按语言代码查询启用的语言。
     *
     * @param code 语言代码(如 zh_CN)
     * @return 语言项；查无结果或未启用返回 null
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/locale/getLocaleByCode")
    fun getLocaleByCode(@RequestParam code: String): SysLocaleCacheEntry?

    /**
     * 列出所有启用的语言（按 sort_no 升序）。
     *
     * @return 启用的语言列表
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/locale/listActiveLocales")
    fun listActiveLocales(): List<SysLocaleCacheEntry>

}
