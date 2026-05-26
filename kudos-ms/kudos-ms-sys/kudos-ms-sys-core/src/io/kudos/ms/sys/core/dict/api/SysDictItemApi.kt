package io.kudos.ms.sys.core.dict.api

import io.kudos.ms.sys.common.dict.api.ISysDictItemApi
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service


/**
 * Local implementation of the dictionary item API.
 *
 * @author K
 * @since 1.0.0
 */
@Primary
@Service
open class SysDictItemApi : ISysDictItemApi
