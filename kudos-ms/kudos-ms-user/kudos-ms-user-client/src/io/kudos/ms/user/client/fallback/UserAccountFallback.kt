package io.kudos.ms.user.client.fallback

import io.kudos.ms.user.client.proxy.IUserAccountProxy
import org.springframework.stereotype.Component


/**
 * 用户容错处理
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
interface UserAccountFallback : IUserAccountProxy {



}
