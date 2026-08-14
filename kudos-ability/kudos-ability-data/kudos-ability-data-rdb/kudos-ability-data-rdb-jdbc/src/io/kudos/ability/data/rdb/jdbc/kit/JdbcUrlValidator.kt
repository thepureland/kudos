package io.kudos.ability.data.rdb.jdbc.kit

/**
 * Rejects JDBC connection urls carrying connection parameters that turn "may configure a data
 * source" into "may read arbitrary files on, or execute code inside, the application server".
 *
 * Why this exists: data source urls in this framework are not always operator-authored. They also
 * arrive from the sys console (`sys_datasource` rows, connectivity tests) and from
 * [io.kudos.ability.data.rdb.jdbc.datasource.IDynamicDataSourceLoad] implementations reading such
 * rows. JDBC drivers accept parameters that make the *client* do dangerous things when it talks to
 * a server the attacker controls, so a url is an executable payload, not just an address:
 *
 *  - MySQL / MariaDB `allowLoadLocalInfile` + a malicious server → server asks the client for a
 *    local file (`LOCAL INFILE`) and the driver hands it over: arbitrary file read on the app host.
 *  - MySQL `autoDeserialize`, `queryInterceptors`, `statementInterceptors`, `propertiesTransform` →
 *    Java deserialization / arbitrary class loading: remote code execution.
 *  - PostgreSQL `socketFactory` / `sslfactory` (+ their `*Arg` variants) → instantiate an
 *    arbitrary class with an attacker-chosen argument: remote code execution.
 *  - PostgreSQL `loggerFile` → write a file at an attacker-chosen path.
 *  - H2 `INIT` / `RUNSCRIPT` → run arbitrary SQL (and therefore arbitrary Java) at connect time.
 *
 * Policy is a **deny list, not an allow list**: connection parameters are open-ended and vendor
 * specific, so allow-listing would break legitimate tuning (`characterEncoding`, `currentSchema`,
 * `ApplicationName`, …). Only parameters with no legitimate use in a server-side connection string
 * are refused. Parameters that merely widen an existing risk without adding a new capability (e.g.
 * `allowMultiQueries`) are deliberately **not** refused — that would break working deployments for
 * little gain.
 *
 * This does not make an arbitrary url safe: it still points the application at whatever host the
 * author chose. Restricting *which hosts* are reachable is a network/allow-list concern that
 * belongs to the deployment, not to this validator.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object JdbcUrlValidator {

    /**
     * Connection parameter names (lower-cased) that are refused. See the class KDoc for what each
     * family buys an attacker.
     */
    private val DENIED_PARAMETERS: Set<String> = setOf(
        // MySQL / MariaDB — client-side file read
        "allowloadlocalinfile", "allowloadlocalinfileinpath", "allowurlinlocalinfile",
        "uselocalinfile", "localinfile",
        // MySQL / MariaDB — deserialization + arbitrary class loading
        "autodeserialize", "queryinterceptors", "statementinterceptors", "propertiestransform",
        // PostgreSQL — arbitrary class instantiation / file write
        "socketfactory", "socketfactoryarg", "sslfactory", "sslfactoryarg",
        "sslhostnameverifier", "loggerfile",
        // H2 — arbitrary SQL at connect time
        "init", "runscript",
    )

    /**
     * Throws [IllegalArgumentException] if [url] carries a denied connection parameter.
     *
     * @param url the JDBC url to inspect
     * @throws IllegalArgumentException naming the offending parameter(s); the url itself is not
     *   echoed because it usually embeds credentials
     */
    fun validate(url: String) {
        val denied = deniedParametersIn(url)
        require(denied.isEmpty()) {
            "Refusing JDBC url with unsafe connection parameter(s): ${denied.sorted().joinToString()}. " +
                    "These let a connection string read local files or execute code on this host. " +
                    "If a driver-level feature is genuinely required, build the DataSource explicitly " +
                    "instead of going through DataSourceKit / RdbKit."
        }
    }

    /** Non-throwing form of [validate]: true when the url carries no denied parameter. */
    fun isSafe(url: String): Boolean = deniedParametersIn(url).isEmpty()

    /**
     * Returns the denied parameter names found in [url].
     *
     * JDBC urls carry parameters in two shapes — `?a=1&b=2` (MySQL / PostgreSQL) and `;a=1;b=2`
     * (H2 / SQL Server) — and a value may itself contain a separator. Splitting on every separator
     * over-segments such values, which only ever makes the scan more suspicious, never less, so it
     * is the safe direction to err in.
     */
    private fun deniedParametersIn(url: String): Set<String> =
        url.split('?', '&', ';', '#')
            .asSequence()
            .filter { '=' in it }
            .map { it.substringBefore('=').trim().lowercase() }
            .filter { it in DENIED_PARAMETERS }
            .toSet()
}
