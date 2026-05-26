package io.kudos.ms.sys.common.domain.api

import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * External API for domains.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDomainApi {


    /**
     * Returns the domain information for the given name.
     *
     * @param domainName domain name
     * @return cached domain entry, or null if not found
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/domain/getDomainByName")
    fun getDomainByName(@RequestParam domainName: String): SysDomainCacheEntry?


}
