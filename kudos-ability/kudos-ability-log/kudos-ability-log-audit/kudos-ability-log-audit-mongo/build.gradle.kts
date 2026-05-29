dependencies {
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common"))

    // docdb-mongo provides the spring-data-mongodb starter dependency + BigIntegerConverters /
    // MongoTemplate wiring; this module rides on it so apps never need to depend on Mongo
    // infrastructure twice.
    api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-docdb:kudos-ability-data-docdb-mongo"))

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
}
