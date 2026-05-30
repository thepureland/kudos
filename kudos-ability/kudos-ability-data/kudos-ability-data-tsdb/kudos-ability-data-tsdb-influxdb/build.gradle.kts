dependencies {
    api(project(":kudos-context"))

    // InfluxDB 2.x Java client; provides InfluxDBClient + InfluxDBClientFactory used by the
    // auto-config. Pulled api so apps using InfluxDBClient don't need to redeclare the dep.
    api(libs.influxdb.client.java)

    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
}
