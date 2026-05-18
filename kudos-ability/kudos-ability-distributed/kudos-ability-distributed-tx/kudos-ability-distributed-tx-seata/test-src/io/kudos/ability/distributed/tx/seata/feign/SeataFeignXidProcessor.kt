package io.kudos.ability.distributed.tx.seata.feign

import feign.RequestTemplate
import io.kudos.ability.distributed.client.feign.support.IFeignRequestContextProcess
import io.kudos.context.core.KudosContext
import org.apache.seata.core.context.RootContext

/**
 * 把当前线程的 Seata 全局事务 XID 注入到 Feign 出站请求头里。
 *
 * 没有这个处理器时，跨服务调用的对端（ms12 / ms22）拿不到 XID，[RootContext.getXID]
 * 是 null，Seata client 会以为自己不在全局事务里 → 各自开自己的局部 commit，
 * 主侧 `@GlobalTransactional` rollback 时找不到分支可回滚，导致 AtSeataTest.remoteTx
 * 的"分支异常应当全部回滚"断言失败（实际余额被改但没还原）。
 *
 * 头名沿用 Seata 约定 [RootContext.KEY_XID]（值 = `"TX_XID"`），服务端
 * [SeataXidServletFilter] 解析回写到 `RootContext`。
 */
class SeataFeignXidProcessor : IFeignRequestContextProcess {

    override fun processContext(requestTemplate: RequestTemplate, context: KudosContext) {
        val xid = RootContext.getXID() ?: return
        requestTemplate.header(RootContext.KEY_XID, xid)
    }
}
