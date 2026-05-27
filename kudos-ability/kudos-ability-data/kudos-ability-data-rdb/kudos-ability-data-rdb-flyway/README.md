# kudos-ability-data-rdb-flyway

Multi-data-source Flyway migrator that runs at Spring Boot startup. SQL scripts are organized by
"module × database type"; at startup each module is migrated in the declared order, and any single
module's failure aborts startup (so the app never runs against a half-migrated schema).

## When to use it

- A single process manages multiple RDB data sources (e.g. `master` + `audit_log` + `tenant_*`),
  each with its own set of migrations.
- The same SQL needs to run against multiple database types (h2 for tests / postgresql for prod);
  scripts are split per dbType subdirectory.

If you only have one data source, Spring Boot's stock `spring-boot-starter-flyway` is enough and
this module is overkill.

## Convention

```
classpath:sql/
    └─ <moduleName>/                ← one directory per kudos business module
        └─ <dbType>/                ← postgresql / h2 / mysql ... (RdbTypeEnum#name.lowercase())
            ├─ V1.0.0__init.sql     ← Flyway standard naming
            └─ V1.0.1__add_x.sql
```

Each module uses its own Flyway metadata table `flyway_history_<moduleName>`, so they never
contaminate one another.

## Configuration

```yaml
kudos:
  ability:
    flyway:
      enabled: true                # set to false to disable startup migration entirely
                                   # (e.g. read-only replicas)
      datasource-config:           # ds → module list, migrated in declaration order
        master: sys,tenant         # CSV form
        audit:                     # list form
          - audit_log
      execution-order:             # optional: explicitly override ds execution order;
        - master                   # unlisted entries keep their original relative order
        - audit                    # and follow at the end
      auto-config:
        enabled: false             # see "Two modes" below

# Flyway's own parameters live under the standard Spring Boot prefix
spring:
  flyway:
    baseline-on-migrate: true
    encoding: UTF-8
    out-of-order: false
    validate-on-migrate: true
    placeholder-replacement: true
    placeholders:
      app_schema: public
```

`kudos.ability.flyway.*` and `spring.flyway.*` do not overlap: the former only decides "which data
source runs which modules and in what order"; the latter controls Flyway's own behavior
(baseline / encoding / outOfOrder / placeholders ...).

## Two modes

| Mode | `auto-config.enabled` | Behavior |
|---|---|---|
| Manual (default) | `false` | Migrate only the modules listed in `datasource-config`; modules on disk but not declared are silently skipped; modules declared but missing on disk are warned. |
| Auto-scan | `true` | Scan `classpath:sql/*`; **every discovered module must have a ds mapping under `datasource-config`**, otherwise startup aborts. (Auto only relaxes discovery, not the mapping decision.) |

## Module entry points

| Class | Role |
|---|---|
| `FlywayAutoConfiguration` | Wiring entry; `@ConditionalOnProperty(kudos.ability.flyway.enabled, default=true)` controls whether it activates. |
| `FlywayMultiDataSourceMigrator` | Startup-time migrator; scans classpath, reconciles with properties, runs migrations by ds in order. `migrateByModule(name)` is the single-module entry point. |
| `FlywayMultiDataSourceProperties` | yml binding for `ds → modules`; values may be CSV strings or YAML lists. |
| `FlywayPreConfiguration` + `FlywayModuleStrategy` | Suppress Spring Boot's default Flyway behavior (otherwise it would try to migrate `classpath:db/migration` against the primary data source). |
| `FlywayKit` | Pure-function single-module migration; **also usable outside Spring** (code generators / CLI tools can call it directly). |

## Failure semantics

- Flyway `migrate()` reports `success=false` → throws `IllegalStateException` and aborts startup.
- The data source key configured for a module does not exist → throws `IllegalStateException`; the
  error message lists which yml file(s) the `datasource-config` came from.
- The same module name appears under multiple classpath URLs → throws `IllegalStateException`.
- A module declared in config has no scripts on disk → logs a warning and continues.
- With `auto-config.enabled=true`, a module exists on disk but has no mapping in `datasource-config`
  → throws `IllegalStateException`.
- A `execution-order` mis-indented under `datasource-config` is ignored by the reserved-key
  defense, never interpreted as a ds name.
- `spring.flyway.placeholders` / `placeholder-prefix` / `placeholder-suffix` / `placeholder-separator`
  are all propagated to every per-module Flyway instance.

Design principle: **better to fail startup than to run the app against an inconsistent schema**.

## Error tracing

When migration fails, the error message lists which configuration sources contributed
`kudos.ability.flyway.datasource-config.*` entries (powered by
`YamlPropertySourceFactory.getSourceMap()`). In multi-jar / multi-yml deployments this lets you
quickly pinpoint which dependency wrote the offending config.

## Dependencies

```kotlin
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
api(libs.spring.boot.starter.flyway)
api(libs.flyway.database.postgresql)        // Flyway 10+ split the PG adapter into its own artifact;
                                            // without it, startup against PG (incl. 18) throws "Unsupported Database".
api(libs.baomidou.dynamic.datasource.starter)
```

Data sources are resolved via `DsContextProcessor`, so this module **currently depends on the
baomidou dynamic-datasource starter**. Decoupling would require abstracting "look up DataSource by
key" into a SPI.

## Known limitations / future work

- ❗ No Flyway callback / hook surface exposed (pre-migrate / post-migrate).
- ✅ Flyway placeholders are propagated: `placeholders / prefix / suffix / separator` all reach
  every per-module Flyway instance.
- ❗ Tightly coupled to `DsContextProcessor` — this module is unusable without baomidou
  dynamic-datasource.
- ❗ No dry-run / repair / clean entry points; ops scripts must bypass this module and use the
  Flyway CLI directly.
- ❗ Test coverage: happy path, missing data source, placeholder substitution, CSV/List parsing,
  execution-order. The jar-protocol path scan, duplicate-module detection, and auto-mode
  missing-mapping branches are not directly unit-tested yet.

## Example: running migrations outside Spring (code generator scenario)

```kotlin
val ds = HikariDataSource(/* ... */)
val flywayProps = FlywayProperties().apply {
    isBaselineOnMigrate = true
    encoding = "UTF-8"
}
FlywayKit.migrate(moduleName = "sys", dataSource = ds, flywayProperties = flywayProps)
```
