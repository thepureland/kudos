package io.kudos.ability.distributed.client.feign.support

import feign.RequestTemplate
import io.kudos.context.core.KudosContext

/**
 * Feign 请求头扩展 SPI。
 *
 * `GlobalHeaderRequestInterceptor` 会把 [KudosContext] 里的常规字段（tenantId / subSysCode /
 * traceKey 等）自动写入请求头；**那些"必须跨服务传但状态不在 KudosContext 里"的数据**
 * （典型例子：Seata 全局事务 XID 放在 `RootContext` 而不是 `KudosContext`）需要通过本 SPI 注入。
 *
 * 当前生产中实现：`SeataFeignXidProcessor`（`kudos-ability-distributed-tx-seata` 模块）
 * 把 `RootContext.getXID()` 写到 `TX_XID` 请求头，配套服务端的 `SeataXidServletFilter`
 * 把它 bind 回当前线程的 Seata 上下文。
 *
 * 装配为 Spring bean 即生效——`GlobalHeaderRequestInterceptor` 通过
 * `SpringKit.getBeansOfType<IFeignRequestContextProcess>()` 自动发现所有实现。
 *
 * @author K
 * @since 1.0.0
 */
interface IFeignRequestContextProcess {
    /**
     * Feign 请求头扩展点。
     *
     * @param requestTemplate Feign 请求模板，向上面 `.header(...)` 写需要透传的头
     * @param context 当前 Kudos 上下文（不一定包含要透传的状态——业务侧可从 ThreadLocal /
     *   `RootContext` 等其他来源读）
     */
    fun processContext(requestTemplate: RequestTemplate, context: KudosContext)
}
