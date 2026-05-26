package io.kudos.ability.web.springmvc.support.enums

/**
 * Enum of embedded servlet containers supported by Spring Boot.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class ServletServerEnum {
    TOMCAT,
    JETTY,
//    UNDERTOW  // Does not support servlet 6.1; excluded by Spring Boot 4
}
