package io.kudos.ms.sys.core.cache.api

import io.kudos.ms.sys.common.cache.api.ISysCacheApi
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * 缓存 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Primary
@Service
open class SysCacheApi : ISysCacheApi
