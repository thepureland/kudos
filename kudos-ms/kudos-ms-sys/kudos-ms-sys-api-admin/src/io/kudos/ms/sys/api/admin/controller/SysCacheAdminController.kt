package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.cache.SysCacheCacheEntry
import io.kudos.ms.sys.common.vo.cache.SysCacheForm
import io.kudos.ms.sys.common.vo.cache.SysCacheQuery
import io.kudos.ms.sys.common.vo.cache.SysCacheRow
import io.kudos.ms.sys.core.service.iservice.ISysCacheService
import org.springframework.web.bind.annotation.*

/**
 * 缓存管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/cache")
open class SysCacheAdminController :
    BaseCrudController<String, ISysCacheService, SysCacheQuery, SysCacheRow, SysCacheCacheEntry, SysCacheForm>() {

    /**
     * 重载指定缓存配置（按 id）下某 key 的缓存项
     *
     * @param id 缓存配置主键
     * @param key 缓存 key
     */
    @GetMapping("/reload")
    fun reload(id: String, key: String) {
        return service.reload(id, key)
    }

    /**
     * 重载指定缓存配置（按 id）下的所有缓存项
     *
     * @param id 缓存配置主键
     */
    @GetMapping("/reloadAll")
    fun reloadAll(id: String) {
        return service.reloadAll(id)
    }

    /**
     * 踢除指定缓存配置（按 id）下某 key 的缓存项
     *
     * @param id 缓存配置主键
     * @param key 缓存 key
     */
    @DeleteMapping("/evict")
    fun evict(id: String, key: String) {
        return service.evict(id, key)
    }

    /**
     * 踢除指定缓存配置（按 id）下的所有缓存项
     *
     * @param id 缓存配置主键
     */
    @DeleteMapping("/evictAll")
    fun evictAll(id: String) {
        return service.evictAll(id)
    }

    /**
     * 检测指定缓存配置（按 id）下某 key 是否存在
     *
     * @param id 缓存配置主键
     * @param key 缓存 key
     */
    @GetMapping("/existsKey")
    fun existsKey(id: String, key: String): Boolean {
        return service.existsKey(id, key)
    }

    /**
     * 获取指定缓存配置（按 id）下某 key 的值的 json 表示
     *
     * @param id 缓存配置主键
     * @param key 缓存 key
     * @return value 的 json 串，value 为 null 或出错返回空串
     */
    @GetMapping("/getValueJson")
    fun getValueJson(id: String, key: String): String {
        return service.getValueJson(id, key)
    }

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