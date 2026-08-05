package io.kudos.ms.sys.api.admin.controller.cache

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.cache.vo.response.SysCacheDetail
import io.kudos.ms.sys.common.cache.vo.response.SysCacheEdit
import io.kudos.ms.sys.common.cache.vo.request.SysCacheFormCreate
import io.kudos.ms.sys.common.cache.vo.request.SysCacheFormUpdate
import io.kudos.ms.sys.common.cache.vo.request.SysCacheQuery
import io.kudos.ms.sys.common.cache.vo.response.SysCacheRow
import io.kudos.ms.sys.core.cache.service.iservice.ISysCacheService
import org.springframework.web.bind.annotation.*

/**
 * Cache management controller.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/cache")
open class SysCacheAdminController :
    BaseCrudController<String, ISysCacheService, SysCacheQuery, SysCacheRow, SysCacheDetail, SysCacheEdit, SysCacheFormCreate, SysCacheFormUpdate>() {

    /**
     * Update the active status.
     *
     * @param id primary key
     * @param active whether enabled
     * @return whether the update succeeded
     */
    @PutMapping("/updateActive")
    fun updateActive(id: String, active: Boolean): Boolean {
        return service.updateActive(id, active)
    }

    /**
     * Reload the cache entry for a given key under the specified cache configuration (by id).
     *
     * @param id cache configuration primary key
     * @param key cache key
     */
    @GetMapping("/management/reload")
    fun reload(id: String, key: String) {
        return service.reload(id, key)
    }

    /**
     * Reload all cache entries under the specified cache configuration (by id).
     *
     * @param id cache configuration primary key
     */
    @GetMapping("/management/reloadAll")
    fun reloadAll(id: String) {
        return service.reloadAll(id)
    }

    /**
     * Evict the cache entry for a given key under the specified cache configuration (by id).
     *
     * @param id cache configuration primary key
     * @param key cache key
     */
    @DeleteMapping("/management/evict")
    fun evict(id: String, key: String) {
        return service.evict(id, key)
    }

    /**
     * Evict all cache entries under the specified cache configuration (by id).
     *
     * @param id cache configuration primary key
     */
    @DeleteMapping("/management/evictAll")
    fun evictAll(id: String) {
        return service.evictAll(id)
    }

    /**
     * Check whether a given key exists under the specified cache configuration (by id).
     *
     * @param id cache configuration primary key
     * @param key cache key
     */
    @GetMapping("/management/existsKey")
    fun existsKey(id: String, key: String): Boolean {
        return service.existsKey(id, key)
    }

    /**
     * Get the JSON representation of the value for a given key under the specified cache configuration (by id).
     *
     * Caches holding sensitive values (e.g. the data source cache `SYS_DATA_SOURCE__HASH`, whose
     * entries embed encrypted DB passwords) are blacklisted at the service layer: requesting them
     * fails with error code `CACHE_VALUE_EXPORT_FORBIDDEN` instead of leaking credentials.
     *
     * @param id cache configuration primary key
     * @param key cache key
     * @return JSON string of the value; returns empty string when the value is null or an error occurs
     */
    @GetMapping("/management/getValueJson")
    fun getValueJson(id: String, key: String): String {
        return service.getValueJson(id, key)
    }

}