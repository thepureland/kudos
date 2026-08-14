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
        ├─ common/                  ← OPTIONAL: scripts shared by every database type
        │   └─ V1.0.0__init.sql
        └─ <dbType>/                ← postgresql / h2 / mysql ... (RdbTypeEnum#name.lowercase())
            ├─ V1.0.1__schema.sql   ← Flyway standard naming
            └─ V1.0.2__add_x.sql
```

`common/` and `<dbType>/` merge into one version stream per module (one history table), so a
version number may live in either directory but not both. SQL identical across database types
belongs in `common/`; only the dialect-specific parts need per-dbType copies.

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
      mode: migrate                # optional: "migrate" (default) applies at startup;
                                   # "dry-run" only logs pending migrations, applies nothing
      flyway-config:               # Flyway's own parameters (see the full list below)
        baseline-on-migrate: true
        encoding: UTF-8
        out-of-order: false
        validate-on-migrate: false
        placeholder-replacement: true
        placeholders:
          app_schema: public
      datasource-flyway-config:    # optional per-ds overrides of flyway-config;
        audit:                     # unset fields inherit the global value,
          out-of-order: true       # placeholders merge per key
          placeholders:
            app_schema: audit
```

### ⚠️ `spring.flyway.*` is NOT used

This module never runs Spring Boot's Flyway auto-configuration — it builds one Flyway instance per
module — and since it depends on `flyway-core` alone, that auto-configuration is not even on the
classpath. **All Flyway parameters live under `kudos.ability.flyway.flyway-config.*`**; any
`spring.flyway.*` key found in the environment is reported at startup:

```
kudos-ability-data-rdb-flyway ignores these spring.flyway.* properties: [...]
```

(`spring.flyway.enabled` is exempt: this module sets it to `false` itself so Boot's
auto-configuration stays inert should its starter ever land on the classpath transitively.)

### Supported parameters

`flyway-config` (class `FlywayConfig`) IS the contract — every field reaches every per-module
Flyway instance, and anything not listed here is deliberately out of scope (use an
`IFlywayModuleConfigCustomizer` bean, see "Extension points"):

| Key | Kudos default | Note |
|---|---|---|
| `baseline-on-migrate` | `true` | |
| `baseline-version` | `0` | |
| `encoding` | `UTF-8` | |
| `out-of-order` | `false` | |
| `validate-on-migrate` | `false` | |
| `fail-on-missing-locations` | `true` | Flyway's own default is `false`; a missing `sql/<module>/<dbType>` fails startup instead of silently reporting "up to date" against an empty schema. |
| `clean-disabled` | `true` | An application has no business wiping its own schema. |
| `placeholder-replacement` | `false` | |
| `placeholders` | empty | Merged per key by `datasource-flyway-config`. |
| `placeholder-prefix` / `-suffix` / `-separator` | `${` / `}` / `:` | |

## Two modes

| Mode | `auto-config.enabled` | Behavior |
|---|---|---|
| Manual (default) | `false` | Migrate only the modules listed in `datasource-config`; modules on disk but not declared are silently skipped; modules declared but missing on disk are warned. |
| Auto-scan | `true` | Scan `classpath:sql/*`; **every discovered module must have a ds mapping under `datasource-config`**, otherwise startup aborts. (Auto only relaxes discovery, not the mapping decision.) |

## Module entry points

| Class | Role |
|---|---|
| `FlywayAutoConfiguration` | Wiring entry; `@ConditionalOnProperty(kudos.ability.flyway.enabled, default=true)` controls whether it activates. |
| `FlywayMultiDataSourceMigrator` | Startup-time migrator; scans classpath, reconciles with properties, runs migrations by ds in order. `migrateByModule(name[, dsKey])` is the single-module entry (the 2-arg form targets runtime-registered data sources, e.g. tenant onboarding); `repairByModule(name[, dsKey])` repairs one module's history (failed records / checksum drift); `previewByModule(name, dsKey)` logs pending migrations without applying. |
| `FlywayMultiDataSourceProperties` | yml binding for everything under `kudos.ability.flyway.*`: `ds → modules` (CSV strings or YAML lists), execution order/mode, `flyway-config` and its per-ds overrides. |
| `FlywayConfig` | The supported Flyway parameters, owned by kudos (not Spring Boot's `FlywayProperties`) — every field reaches every per-module Flyway instance. |
| `IFlywayDataSourceResolver` | SPI "dsKey → DataSource"; the default (`DsContextFlywayDataSourceResolver`) delegates to the baomidou dynamic-routing table and backs off when the app registers its own resolver bean. |
| `IFlywayModuleConfigCustomizer` | Extension point: per-module `FluentConfiguration` hook, applied after the property mapping (callbacks, javaMigrations, extra locations, per-module schemas, ...). |
| `FlywayMigratorDatabaseInitializerDetector` | Registers the migrator with Spring Boot's database-initialization dependency mechanism (see "Startup ordering"). |
| `FlywayKit` | Pure-function single-module `migrate` / `pendingMigrations` / `repair`; **also usable outside Spring** (code generators / CLI tools can call it directly). |

## Startup ordering

`FlywayMigratorDatabaseInitializerDetector` (registered via `META-INF/spring.factories`) plugs the
migrator into Spring Boot's `DatabaseInitializerDetector` mechanism: beans detected as depending on
database initialization — `@DependsOnDatabaseInitialization`-annotated beans, plus types Boot
detects out of the box (JdbcTemplate / JdbcClient ...) — automatically get a `dependsOn` edge to
the migrator, i.e. they are created only after all module migrations finished. Beans not covered
by any built-in detector that touch the database during initialization should be annotated with
`@DependsOnDatabaseInitialization` (from `org.springframework.boot.sql.init.dependency`).

## Extension points

```kotlin
// Anything flyway-config doesn't cover goes through a customizer bean:
@Bean
fun auditFlywayCustomizer() = IFlywayModuleConfigCustomizer { moduleName, config ->
    if (moduleName == "audit_log") {
        config.schemas("audit")                      // per-module schema
        config.callbacks(MyFlywayCallback())         // Flyway callbacks
    }
}

// Non-baomidou environments implement the resolver SPI instead of DsContextProcessor:
@Bean
fun myResolver() = object : IFlywayDataSourceResolver {
    override fun hasDataSource(datasourceKey: String) = ...
    override fun getDataSource(datasourceKey: String) = ...
}
```

## Failure semantics

- Flyway `migrate()` reports `success=false` → throws `IllegalStateException` and aborts startup.
- A configured module's `sql/<module>/<dbType>` directory is missing → aborts startup
  (`fail-on-missing-locations` defaults to true here; override under `flyway-config`).
- A module name contains characters outside `[A-Za-z0-9_-]` → rejected before touching the
  database (the name is embedded into the history table name and the classpath location).
- The data source key configured for a module does not exist → throws `IllegalStateException`; the
  error message lists which yml file(s) the `datasource-config` came from.
- The same module name appears under multiple classpath URLs → throws `IllegalStateException`.
- A module declared in config has no scripts on disk → logs a warning and continues.
- With `auto-config.enabled=true`, a module exists on disk but has no mapping in `datasource-config`
  → throws `IllegalStateException`.
- A `execution-order` mis-indented under `datasource-config` is ignored by the reserved-key
  defense, never interpreted as a ds name.
- An unknown `kudos.ability.flyway.mode` value → throws `IllegalStateException` naming the value.
- `spring.flyway.*` keys present in the environment → logged as ignored at startup (they are not
  used by this module); everything under `flyway-config` reaches every per-module Flyway instance.

Design principle: **better to fail startup than to run the app against an inconsistent schema**.

## Error tracing

When migration fails, the error message lists which configuration sources contributed
`kudos.ability.flyway.datasource-config.*` entries (powered by
`YamlPropertySourceFactory.getSourceMap()`). In multi-jar / multi-yml deployments this lets you
quickly pinpoint which dependency wrote the offending config.

## Dependencies

```kotlin
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc"))
api(libs.flyway.core)                       // the engine only — Spring Boot's Flyway auto-configuration
                                            // is deliberately NOT a dependency (see "spring.flyway.* is NOT used")
api(libs.flyway.database.postgresql)        // Flyway 10+ split the PG adapter into its own artifact;
                                            // without it, startup against PG (incl. 18) throws "Unsupported Database".
api(libs.baomidou.dynamic.datasource.starter)
```

Data sources are resolved via the `IFlywayDataSourceResolver` SPI. The **default** implementation
delegates to `DsContextProcessor` (baomidou dynamic-datasource); registering your own resolver
bean replaces it, so the migrator itself no longer assumes baomidou.

## Known limitations / future work

- ✅ Callbacks / javaMigrations / per-module overrides: covered by `IFlywayModuleConfigCustomizer`.
- ✅ Flyway placeholders are propagated: `placeholders / prefix / suffix / separator` all reach
  every per-module Flyway instance.
- ✅ Decoupled from baomidou via `IFlywayDataSourceResolver` (default impl still delegates to
  `DsContextProcessor`).
- ✅ Startup ordering guaranteed via `DatabaseInitializerDetector` (beans not covered by Boot's
  built-in detectors still need `@DependsOnDatabaseInitialization`).
- ✅ Dry-run (`mode: dry-run` / `previewByModule`) and repair (`repairByModule` / `FlywayKit.repair`)
  entry points; only `clean` remains intentionally unsupported (use the Flyway CLI).
- ✅ Shared `sql/<module>/common` directory merges into each dbType's version stream.
- ✅ Per-data-source overrides of `flyway-config` via `datasource-flyway-config`.
- ✅ The supported parameters are an explicit, owned type (`FlywayConfig`) instead of a borrowed
  Spring Boot one, and stray `spring.flyway.*` keys are reported at startup.
- ❗ `clean` is intentionally unsupported (and `clean-disabled` defaults to true) — use the Flyway
  CLI for destructive operations.

## Example: running migrations outside Spring (code generator scenario)

```kotlin
val ds = HikariDataSource(/* ... */)
val config = FlywayConfig().apply {
    baselineOnMigrate = true
    encoding = "UTF-8"
    placeholderReplacement = false
}
FlywayKit.migrate(moduleName = "sys", dataSource = ds, config = config)
```

`FlywayConfig` has no Spring dependency and its defaults are the kudos defaults, so
`FlywayKit.migrate("sys", ds)` works standalone too.
