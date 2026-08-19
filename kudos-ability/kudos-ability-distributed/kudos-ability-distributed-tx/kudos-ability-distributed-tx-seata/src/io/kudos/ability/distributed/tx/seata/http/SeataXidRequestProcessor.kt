package io.kudos.ability.distributed.tx.seata.http

import io.kudos.ability.distributed.client.http.support.IHttpRequestContextProcess
import io.kudos.context.core.KudosContext
import org.apache.seata.core.context.RootContext
import org.springframework.http.HttpRequest

/**
 * Inject the current thread's Seata global transaction XID into outbound interface-client request headers.
 *
 * Without this processor, the remote side of a cross-service call (ms12 / ms22) cannot obtain the
 * XID, [RootContext.getXID] is null, and the Seata client thinks it is outside a global transaction —
 * each side performs its own local commit, and when the upstream `@GlobalTransactional` rolls back
 * there are no branches to roll back. AtSeataTest.remoteTx's "all branches must roll back on
 * exception" assertion then fails (balances are mutated but not restored).
 *
 * The header name follows the Seata convention [RootContext.KEY_XID] (value = `"TX_XID"`); the
 * server-side [SeataXidServletFilter] parses it back into `RootContext`.
 *
 * This is the canonical use of [IHttpRequestContextProcess]: the XID lives in Seata's own
 * `RootContext` ThreadLocal, not in [KudosContext], so it cannot ride along with the standard
 * context headers and needs this extension point.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class SeataXidRequestProcessor : IHttpRequestContextProcess {

    override fun processContext(request: HttpRequest, context: KudosContext) {
        val xid = RootContext.getXID() ?: return
        request.headers.set(RootContext.KEY_XID, xid)
    }
}
