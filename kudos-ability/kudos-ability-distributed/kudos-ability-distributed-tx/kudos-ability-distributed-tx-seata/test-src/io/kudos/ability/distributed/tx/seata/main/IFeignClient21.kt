package io.kudos.ability.distributed.tx.seata.main

import org.springframework.cloud.openfeign.FeignClient

/**
 * Feign client for microservice application 2.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient("ms21")
interface IFeignClient21 : IClient2
