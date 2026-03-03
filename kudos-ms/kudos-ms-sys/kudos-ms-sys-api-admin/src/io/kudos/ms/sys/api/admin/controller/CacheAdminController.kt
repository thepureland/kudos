package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.cache.SysCacheDetail
import io.kudos.ms.sys.common.vo.cache.SysCachePayload
import io.kudos.ms.sys.common.vo.cache.SysCacheRecord
import io.kudos.ms.sys.common.vo.cache.SysCacheSearchPayload
import io.kudos.ms.sys.core.service.impl.SysCacheService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 缓存管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/cache")
class CacheAdminController :
    BaseCrudController<String, SysCacheService, SysCacheSearchPayload, SysCacheRecord, SysCacheDetail, SysCachePayload>() {

    /**
     * 重载指定缓存名称和key的缓存项
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    fun reload(name: String, key: String) {

    }

    /**
     * 重载指定缓存名称的所有缓存项
     *
     * @param name 缓存名称
     */
    fun reloadAll(name: String) {

    }

    /**
     * 踢除指定缓存名称和key的缓存项
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    fun evict(name: String, key: String) {

    }

    /**
     * 踢除指定缓存名称的所有缓存项
     *
     * @param name 缓存名称
     */
    fun evictAll(name: String) {

    }

    /**
     * 检测指定名称和key的缓存项是否存在
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    fun existKey(name: String, key: String): Boolean {

    }

    /**
     * 获取指定名称和key的缓存项的值的信息
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    fun getValueInfo(name: String, key: String): Any? {

    }

}