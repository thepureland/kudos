
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

include("kudos-ms")



include("kudos-test")
include("kudos-test:kudos-test-common")
findProject(":kudos-test:kudos-test-common")?.name = "kudos-test-common"

