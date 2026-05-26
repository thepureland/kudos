package io.kudos.ability.distributed.config.nacos

import com.alibaba.cloud.nacos.NacosPropertySourceRepository
import io.kudos.ability.distributed.config.nacos.decrypt.NacosConfigValueDecryptor
import io.kudos.context.config.IConfigDataFinder
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import java.util.ServiceLoader

/**
 * Adapts Spring Cloud Alibaba's [NacosPropertySourceRepository] to kudos's [IConfigDataFinder] SPI.
 *
 * `kudos-context`'s `YamlPropertySourceFactory` discovers config finders via
 * `ServiceLoader.load(IConfigDataFinder::class.java)`; this class looks up entries in Nacos's
 * already-loaded PropertySources by `dataId == name`.
 *
 * **Already registered via `resources/META-INF/services/io.kudos.context.config.IConfigDataFinder`** —
 * as long as this module is on the classpath and the spring-cloud-alibaba Nacos client has pulled
 * a PropertySource, `YamlPropertySourceFactory.loadFromConfigCenter` will prefer Nacos content
 * over local yml.
 *
 * To opt out, application code can override the SPI (a jar with higher ServiceLoader priority) or
 * exclude this module entirely.
 *
 * @author hanson
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class NacosConfigDataFinder(
    private val decryptors: List<NacosConfigValueDecryptor> =
        ServiceLoader.load(NacosConfigValueDecryptor::class.java).toList()
) : IConfigDataFinder {

    override fun findConfigData(name: String?): PropertySource<*>? =
        NacosPropertySourceRepository.getAll()
            .firstOrNull { it.dataId == name }
            ?.let(::decryptIfNecessary)

    private fun decryptIfNecessary(source: PropertySource<*>): PropertySource<*> {
        if (decryptors.isEmpty() || source !is MapPropertySource) {
            return source
        }
        val decrypted = source.source.mapValues { (_, value) ->
            if (value is String) {
                decryptors.firstOrNull { it.supports(value) }?.decrypt(value) ?: value
            } else {
                value
            }
        }
        return MapPropertySource(source.name, decrypted)
    }
}
