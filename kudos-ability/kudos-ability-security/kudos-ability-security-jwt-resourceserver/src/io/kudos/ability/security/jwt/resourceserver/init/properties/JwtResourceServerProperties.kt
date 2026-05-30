package io.kudos.ability.security.jwt.resourceserver.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ability.security.jwt.resource-server")
class JwtResourceServerProperties {
    var enabled: Boolean = false
    var permittedPaths: List<String> = emptyList()
}
