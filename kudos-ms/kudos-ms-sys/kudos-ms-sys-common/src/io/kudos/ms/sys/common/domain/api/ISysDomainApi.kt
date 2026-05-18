package io.kudos.ms.sys.common.domain.api

import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 域名 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDomainApi {


    /**
     * 返回指定名称的域名信息
     *
     * @param domainName 域名名称
     * @return 域名信息缓存项，如果找不到返回null
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/domain/getDomainByName")
    fun getDomainByName(@RequestParam domainName: String): SysDomainCacheEntry?


}
