package io.kudos.ability.distributed.client.feign.fallback

import feign.FeignException

/**
 * Test-only [FeignException] subclass exposing the protected (status, message) constructor,
 * so tests can build exceptions with arbitrary status codes (0 / 4xx / 5xx / out-of-range).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class TestFeignException(status: Int, message: String = "stub-feign-exception") :
    FeignException(status, message)
