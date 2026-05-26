package io.kudos.ability.distributed.tx.seata.main

import org.springframework.cloud.openfeign.FeignClient

/**
 * Feign client for microservice application 1.
 *
 * @author K
 * @since 1.0.0
 */
@FeignClient("ms11")
interface IFeignClient11 : IClient1
