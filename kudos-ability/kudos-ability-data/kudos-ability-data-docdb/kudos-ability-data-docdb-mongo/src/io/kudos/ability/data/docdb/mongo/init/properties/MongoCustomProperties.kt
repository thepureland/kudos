package io.kudos.ability.data.docdb.mongo.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the kudos-ability-data-docdb-mongo module.
 *
 * Bound from `kudos.ability.docdb.mongo.*` in `application.yml`. The module rides directly on
 * Spring Boot's `spring.data.mongodb.*` for connection / database / credentials — this class only
 * carries kudos-specific toggles layered on top.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.docdb.mongo")
class MongoCustomProperties {

    /**
     * When true, [BigInteger] fields on `@Document` classes are stored as String to preserve
     * arbitrary precision. Default true because it's the only loss-free way to round-trip
     * BigInteger through BSON; flip to false when the app prefers BSON's native Decimal128 (with
     * its 34-digit precision ceiling) or never uses BigInteger at all.
     *
     * See [io.kudos.ability.data.docdb.mongo.convert.BigIntegerConverters] for the trade-off
     * (numeric range queries on the field become lexical).
     */
    var bigIntegerAsString: Boolean = true
}
