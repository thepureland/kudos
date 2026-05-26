package io.kudos.ability.distributed.tx.seata.feign

import feign.RequestTemplate
import io.kudos.ability.distributed.client.feign.support.IFeignRequestContextProcess
import io.kudos.context.core.KudosContext
import org.apache.seata.core.context.RootContext

/**
 * Inject the current thread's Seata global transaction XID into outbound Feign request headers.
 *
 * Without this processor, the remote side of a cross-service call (ms12 / ms22) cannot obtain the
 * XID, [RootContext.getXID] is null, and the Seata client thinks it is outside a global transaction —
 * each side performs its own local commit, and when the upstream `@GlobalTransactional` rolls back
 * there are no branches to roll back. AtSeataTest.remoteTx's "all branches must roll back on
 * exception" assertion then fails (balances are mutated but not restored).
 *
 * The header name follows the Seata convention [RootContext.KEY_XID] (value = `"TX_XID"`); the
 * server-side [SeataXidServletFilter] parses it back into `RootContext`.
 */
class SeataFeignXidProcessor : IFeignRequestContextProcess {

    override fun processContext(requestTemplate: RequestTemplate, context: KudosContext) {
        val xid = RootContext.getXID() ?: return
        requestTemplate.header(RootContext.KEY_XID, xid)
    }
}
