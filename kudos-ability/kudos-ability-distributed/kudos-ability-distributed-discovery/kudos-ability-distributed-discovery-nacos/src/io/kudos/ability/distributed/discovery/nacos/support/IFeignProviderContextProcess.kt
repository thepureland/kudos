package io.kudos.ability.distributed.discovery.nacos.support

import io.kudos.context.core.KudosContext
import jakarta.servlet.http.HttpServletRequest

/**
 * Provider 端的 Feign 上下文扩展 SPI。
 *
 * 与 client 端的 `IFeignRequestContextProcess`（在 `kudos-ability-distributed-client-feign` 模块）
 * 对偶——client 写入 header 后，provider 端可以通过本 SPI 把这些非 [KudosContext] 字段的 header
 * 解析回 ThreadLocal / 其他全局状态。
 *
 * 典型例子：Seata 全局事务 XID
 *  - client 端 `SeataFeignXidProcessor` 把 `RootContext.getXID()` 写到 `TX_XID` header
 *  - provider 端可实现本 SPI，把 `request.getHeader("TX_XID")` bind 回 `RootContext.bind(xid)`
 *    （生产中走的是 `SeataXidServletFilter`，但通过本 SPI 也是合法路径）
 *
 * 业务侧将实现注册为 Spring bean 即可——[io.kudos.ability.distributed.discovery.nacos.filter.FeignContextWebFilter]
 * 会自动发现并调用所有实现。
 *
 * @author K
 * @since 1.0.0
 */
interface IFeignProviderContextProcess {
    /**
     * 从 HTTP 请求里读取业务自定义的透传 header，写回 [KudosContext] 或其他 ThreadLocal。
     *
     * @param request 当前 HTTP 请求（保证非 null）
     * @param context 当前线程的 KudosContext（可读可写）
     */
    fun processContext(request: HttpServletRequest, context: KudosContext)
}
