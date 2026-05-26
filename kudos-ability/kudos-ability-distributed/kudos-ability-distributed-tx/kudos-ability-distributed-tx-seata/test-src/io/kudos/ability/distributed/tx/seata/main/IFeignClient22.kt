package io.kudos.ability.distributed.tx.seata.main

import org.springframework.cloud.openfeign.FeignClient

/**
 * Feign client for microservice application 2.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient("ms22")
interface IFeignClient22 : IClient2
