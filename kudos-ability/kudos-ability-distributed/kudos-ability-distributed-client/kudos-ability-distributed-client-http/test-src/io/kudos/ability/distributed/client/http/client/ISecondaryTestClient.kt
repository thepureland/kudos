package io.kudos.ability.distributed.client.http.client

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

/**
 * A second client bound to the **same** group as [IHttpTestClient], but declared from a different
 * `@ImportHttpServices`. Exists to prove that same-named groups merge across declaration sites —
 * the property the code generator relies on to emit one self-contained registration per entity
 * instead of a hand-maintained aggregate list.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@HttpExchange("/testHttpService")
interface ISecondaryTestClient {

    @GetExchange("/get")
    fun get(): Boolean

}
