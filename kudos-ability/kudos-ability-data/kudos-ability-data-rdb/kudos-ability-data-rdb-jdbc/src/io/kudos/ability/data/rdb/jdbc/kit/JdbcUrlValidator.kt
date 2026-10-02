package io.kudos.ability.data.rdb.jdbc.kit

import java.util.Locale

/**
 * Validates the restricted JDBC URL syntax accepted from data-source configuration.
 *
 * Only known drivers and reviewed properties are accepted. Encoded property names and driver-specific
 * host attribute grammars are rejected rather than interpreted differently from the actual driver.
 * Credentials belong in the separate user/password fields. Host reachability must additionally be
 * restricted by deployment network policy; this validation does not prevent connections to internal hosts.
 */
object JdbcUrlValidator {
    private val common = setOf("connecttimeout", "sockettimeout", "logintimeout")
    private val allowedParameters = mapOf(
        "mysql" to common + setOf("usessl", "sslmode", "requiressl", "verifyservercertificate",
            "useunicode", "characterencoding", "connectioncollation", "servertimezone", "connectiontimezone",
            "allowpublickeyretrieval", "cacheprepstmts", "prepstmtcachesize", "prepstmtcachesqllimit",
            "useserverprepstmts", "rewritebatchedstatements", "allowmultiqueries", "tcpkeepalive",
            "zerodatetimebehavior", "tinyint1isbit", "useaffectedrows"),
        "mariadb" to common + setOf("usessl", "sslmode", "useunicode", "characterencoding",
            "servertimezone", "allowmultiqueries", "usebulkstmts", "tcpkeepalive"),
        "postgresql" to common + setOf("ssl", "sslmode", "currentschema", "applicationname",
            "tcpkeepalive", "preparethreshold", "preparedstatementcachequeries",
            "preparedstatementcachesizemib", "defaultrowfetchsize", "rewritebatchedinserts",
            "targetservertype", "loadbalancehosts", "hostrecheckseconds", "stringtype"),
        "h2" to setOf("database_to_lower", "database_to_upper", "db_close_delay", "db_close_on_exit",
            "mode", "auto_server", "ifexists", "lock_timeout", "max_memory_rows", "cache_size",
            "case_insensitive_identifiers", "default_null_ordering", "non_keywords"),
        "sqlserver" to common + setOf("databasename", "encrypt", "trustservercertificate",
            "applicationname", "multisubnetfailover", "sendstringparametersasunicode"),
        "clickhouse" to common + setOf("ssl", "sslmode", "database", "compress", "decompress",
            "connection_timeout", "socket_timeout"),
        "oracle" to emptySet(),
    )
    private val host = Regex("(?:[A-Za-z0-9._-]+|\\[[0-9A-Fa-f:]+\\])(?::[0-9]{1,5})?")
    private val propertyName = Regex("[A-Za-z][A-Za-z0-9_]*")

    /** Errors never echo the URL or property values, which can contain credentials. */
    fun validate(url: String) {
        require(url.startsWith("jdbc:") && url.none { it.isISOControl() }) { "Invalid JDBC URL" }
        val driver = url.removePrefix("jdbc:").substringBefore(':').lowercase(Locale.ROOT)
        val allowed = requireNotNull(allowedParameters[driver]) { "Unsupported JDBC URL driver" }
        val location = url.substringBefore('?').substringBefore(';')
        require('#' !in url && location.none { it in "()%\\\"'=" }) {
            "JDBC URL contains unsupported address attributes or escaping"
        }
        if (driver in setOf("mysql", "mariadb", "postgresql", "sqlserver", "clickhouse")) {
            val prefix = "jdbc:$driver://"
            require(location.startsWith(prefix)) { "Unsupported JDBC URL address syntax" }
            val authority = location.removePrefix(prefix).substringBefore('/')
            require(authority.split(',').all { host.matches(it) }) { "Unsupported JDBC URL host syntax" }
        }
        if (driver == "oracle") {
            require(Regex("jdbc:oracle:thin:@(?://)?[A-Za-z0-9._-]+:[0-9]{1,5}[:/][A-Za-z0-9._-]+").matches(location)) {
                "Only direct Oracle thin host connections are accepted"
            }
        }
        val parameterStart = url.indexOfFirst { it == '?' || it == ';' }
        if (parameterStart < 0) return
        url.substring(parameterStart + 1).split('&', ';').filter { it.isNotEmpty() }.forEach { parameter ->
            val rawName = parameter.substringBefore('=')
            require('=' in parameter && propertyName.matches(rawName)) { "Invalid JDBC connection parameter name" }
            val name = rawName.lowercase(Locale.ROOT)
            require(name in allowed) { "Refusing unapproved JDBC connection parameter: $name" }
        }
    }

    fun isSafe(url: String): Boolean = runCatching { validate(url) }.isSuccess
}
