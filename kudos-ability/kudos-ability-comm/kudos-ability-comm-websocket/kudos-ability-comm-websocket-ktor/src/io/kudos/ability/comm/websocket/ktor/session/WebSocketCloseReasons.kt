package io.kudos.ability.comm.websocket.ktor.session

import io.ktor.websocket.CloseReason
import io.kudos.ability.comm.websocket.common.session.WebSocketCloseReason

/**
 * Translation between the engine-neutral [WebSocketCloseReason] and Ktor's [CloseReason].
 *
 * Confined to this file on purpose: it is the single seam where this module converts, so the shared
 * abstractions in `kudos-ability-comm-websocket-common` never name a Ktor type, and a change to
 * either side has exactly one place to touch.
 *
 * Both types carry the same two fields the protocol defines — a `Short` status code and a reason
 * phrase — so the conversion needs no fallback for codes Ktor does not
 * enumerate. Only this direction exists: the SPI never hands a Ktor `CloseReason` back to business
 * code, so a `toKudos` counterpart would be public API with no reader.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */

/** Converts to the close reason Ktor puts on the wire. */
fun WebSocketCloseReason.toKtor(): CloseReason = CloseReason(code, message)

