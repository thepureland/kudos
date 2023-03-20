
rootProject.name = "kudos"

include("kudos-base")

include("kudos-context")

include("kudos-ability")
include("kudos-ability:kudos-ability-data")
findProject(":kudos-ability:kudos-ability-data")?.name = "kudos-ability-data"
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb")?.name = "kudos-ability-data-rdb"
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc")?.name = "kudos-ability-data-rdb-jdbc"
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm")?.name = "kudos-ability-data-rdb-ktorm"
include("kudos-ability:kudos-ability-ui")
findProject(":kudos-ability:kudos-ability-ui")?.name = "kudos-ability-ui"
include("kudos-ability:kudos-ability-ui:kudos-ability-ui-javafx")
findProject(":kudos-ability:kudos-ability-ui:kudos-ability-ui-javafx")?.name = "kudos-ability-ui-javafx"

include("kudos-ms")

include("kudos-tools")

include("kudos-test")
include("kudos-test:kudos-test-common")
findProject(":kudos-test:kudos-test-common")?.name = "kudos-test-common"

