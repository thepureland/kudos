rootProject.name = "kudos"

include("kudos-ability")
include("kudos-ability:kudos-ability-cache")
findProject(":kudos-ability:kudos-ability-cache")?.name = "kudos-ability-cache"
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-common")
findProject(":kudos-ability:kudos-ability-cache:kudos-ability-cache-common")?.name = "kudos-ability-cache-common"
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice")
findProject(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice")?.name = "kudos-ability-cache-interservice"
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client")
findProject(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client")?.name = "kudos-ability-cache-interservice-client"
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider")
findProject(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider")?.name = "kudos-ability-cache-interservice-provider"
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-local")
findProject(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local")?.name = "kudos-ability-cache-local"
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine")
findProject(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine")?.name = "kudos-ability-cache-local-caffeine"
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-remote")
findProject(":kudos-ability:kudos-ability-cache:kudos-ability-cache-remote")?.name = "kudos-ability-cache-remote"
include("kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis")
findProject(":kudos-ability:kudos-ability-cache:kudos-ability-cache-remote:kudos-ability-cache-remote-redis")?.name = "kudos-ability-cache-remote-redis"
include("kudos-ability:kudos-ability-comm")
findProject(":kudos-ability:kudos-ability-comm")?.name = "kudos-ability-comm"
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-common")
findProject(":kudos-ability:kudos-ability-comm:kudos-ability-comm-common")?.name = "kudos-ability-comm-common"
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-email")
findProject(":kudos-ability:kudos-ability-comm:kudos-ability-comm-email")?.name = "kudos-ability-comm-email"
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-netty")
findProject(":kudos-ability:kudos-ability-comm:kudos-ability-comm-netty")?.name = "kudos-ability-comm-netty"
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-sms")
findProject(":kudos-ability:kudos-ability-comm:kudos-ability-comm-sms")?.name = "kudos-ability-comm-sms"
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-sms:kudos-ability-comm-sms-aliyun")
findProject(":kudos-ability:kudos-ability-comm:kudos-ability-comm-sms:kudos-ability-comm-sms-aliyun")?.name = "kudos-ability-comm-sms-aliyun"
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-sms:kudos-ability-comm-sms-aws")
findProject(":kudos-ability:kudos-ability-comm:kudos-ability-comm-sms:kudos-ability-comm-sms-aws")?.name = "kudos-ability-comm-sms-aws"
include("kudos-ability:kudos-ability-comm:kudos-ability-comm-websocket")
findProject(":kudos-ability:kudos-ability-comm:kudos-ability-comm-websocket")?.name = "kudos-ability-comm-websocket"
include("kudos-ability:kudos-ability-data")
findProject(":kudos-ability:kudos-ability-data")?.name = "kudos-ability-data"
include("kudos-ability:kudos-ability-data:kudos-ability-data-docdb")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-docdb")?.name = "kudos-ability-data-docdb"
include("kudos-ability:kudos-ability-data:kudos-ability-data-docdb:kudos-ability-data-docdb-mongo")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-docdb:kudos-ability-data-docdb-mongo")?.name = "kudos-ability-data-docdb-mongo"
include("kudos-ability:kudos-ability-data:kudos-ability-data-memdb")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-memdb")?.name = "kudos-ability-data-memdb"
include("kudos-ability:kudos-ability-data:kudos-ability-data-memdb:kudos-ability-data-memdb-redis")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-memdb:kudos-ability-data-memdb-redis")?.name = "kudos-ability-data-memdb-redis"
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb")?.name = "kudos-ability-data-rdb"
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-flyway")?.name = "kudos-ability-data-rdb-flyway"
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-jdbc")?.name = "kudos-ability-data-rdb-jdbc"
include("kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm")
findProject(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm")?.name = "kudos-ability-data-rdb-ktorm"
include("kudos-ability:kudos-ability-distributed")
findProject(":kudos-ability:kudos-ability-distributed")?.name = "kudos-ability-distributed"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client")?.name = "kudos-ability-distributed-client"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-config")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-config")?.name = "kudos-ability-distributed-config"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery")?.name = "kudos-ability-distributed-discovery"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-lock")?.name = "kudos-ability-distributed-lock"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify")
findProject(":kudos-ability:kudos-ability-distributed-notify")?.name = "kudos-ability-distributed-notify"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-common")?.name = "kudos-ability-distributed-notify-common"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-mq")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-mq")?.name = "kudos-ability-distributed-notify-mq"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-rdb")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-notify:kudos-ability-distributed-notify-rdb")?.name = "kudos-ability-distributed-notify-rdb"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream")?.name = "kudos-ability-distributed-stream"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common")?.name = "kudos-ability-distributed-stream-common"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-kafka")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-kafka")?.name = "kudos-ability-distributed-stream-kafka"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-rabbit")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-rabbit")?.name = "kudos-ability-distributed-stream-rabbit"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-rocketmq")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-rocketmq")?.name = "kudos-ability-distributed-stream-rocketmq"
include("kudos-ability:kudos-ability-distributed:kudos-ability-distributed-tx")
findProject(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-tx")?.name = "kudos-ability-distributed-tx"
include("kudos-ability:kudos-ability-engine")
findProject(":kudos-ability:kudos-ability-engine")?.name = "kudos-ability-engine"
include("kudos-ability:kudos-ability-engine:kudos-ability-engine-workflow")
findProject(":kudos-ability:kudos-ability-engine:kudos-ability-engine-workflow")?.name = "kudos-ability-engine-workflow"
include("kudos-ability:kudos-ability-file")
findProject(":kudos-ability:kudos-ability-file")?.name = "kudos-ability-file"
include("kudos-ability:kudos-ability-file:kudos-ability-file-common")
findProject(":kudos-ability:kudos-ability-file:kudos-ability-file-common")?.name = "kudos-ability-file-common"
include("kudos-ability:kudos-ability-file:kudos-ability-file-local")
findProject(":kudos-ability:kudos-ability-file:kudos-ability-file-local")?.name = "kudos-ability-file-local"
include("kudos-ability:kudos-ability-file:kudos-ability-file-minio")
findProject(":kudos-ability:kudos-ability-file:kudos-ability-file-minio")?.name = "kudos-ability-file-minio"
include("kudos-ability:kudos-ability-file:kudos-ability-file-oss")
findProject(":kudos-ability:kudos-ability-file:kudos-ability-file-oss")?.name = "kudos-ability-file-oss"
include("kudos-ability:kudos-ability-log")
findProject(":kudos-ability:kudos-ability-log")?.name = "kudos-ability-log"
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit")
findProject(":kudos-ability:kudos-ability-log:kudos-ability-log-audit")?.name = "kudos-ability-log-audit"
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common")
findProject(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common")?.name = "kudos-ability-log-audit-common"
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-mongo")
findProject(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-mongo")?.name = "kudos-ability-log-audit-mongo"
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-mq")
findProject(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-mq")?.name = "kudos-ability-log-audit-mq"
include("kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb")
findProject(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb")?.name = "kudos-ability-log-audit-rdb"
include("kudos-ability:kudos-ability-security")
findProject(":kudos-ability:kudos-ability-security")?.name = "kudos-ability-security"
include("kudos-ability:kudos-ability-security:kudos-ability-security-captcha")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-captcha")?.name = "kudos-ability-security-captcha"
include("kudos-ability:kudos-ability-security:kudos-ability-security-captcha:kudos-ability-security-captcha-common")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-captcha:kudos-ability-security-captcha-common")?.name = "kudos-ability-security-captcha-common"
include("kudos-ability:kudos-ability-security:kudos-ability-security-captcha:kudos-ability-security-captcha-common-impl")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-captcha:kudos-ability-security-captcha-common-impl")?.name = "kudos-ability-security-captcha-common-impl"
include("kudos-ability:kudos-ability-security:kudos-ability-security-captcha:kudos-ability-security-captcha-tianai")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-captcha:kudos-ability-security-captcha-tianai")?.name = "kudos-ability-security-captcha-tianai"
include("kudos-ability:kudos-ability-security:kudos-ability-security-captcha:kudos-ability-security-captcha-web")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-captcha:kudos-ability-security-captcha-web")?.name = "kudos-ability-security-captcha-web"
include("kudos-ability:kudos-ability-security:kudos-ability-security-common")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-common")?.name = "kudos-ability-security-common"
include("kudos-ability:kudos-ability-security:kudos-ability-security-core")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-core")?.name = "kudos-ability-security-core"
include("kudos-ability:kudos-ability-security:kudos-ability-security-spring")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-spring")?.name = "kudos-ability-security-spring"
include("kudos-ability:kudos-ability-security:kudos-ability-security-web")
findProject(":kudos-ability:kudos-ability-security:kudos-ability-security-web")?.name = "kudos-ability-security-web"
include("kudos-ability:kudos-ability-ui")
findProject(":kudos-ability:kudos-ability-ui")?.name = "kudos-ability-ui"
include("kudos-ability:kudos-ability-ui:kudos-ability-ui-javafx")
findProject(":kudos-ability:kudos-ability-ui:kudos-ability-ui-javafx")?.name = "kudos-ability-ui-javafx"
include("kudos-ability:kudos-ability-web")
findProject(":kudos-ability:kudos-ability-web")?.name = "kudos-ability-web"
include("kudos-ability:kudos-ability-web:kudos-ability-web-common")
findProject(":kudos-ability:kudos-ability-web:kudos-ability-web-common")?.name = "kudos-ability-web-common"
include("kudos-ability:kudos-ability-web:kudos-ability-web-springmvc")
findProject(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc")?.name = "kudos-ability-web-springmvc"
include("kudos-ability:kudos-ability-web:kudos-ability-web-swagger")
findProject(":kudos-ability:kudos-ability-web:kudos-ability-web-swagger")?.name = "kudos-ability-web-swagger"


include("kudos-base")


include("kudos-context")


include("kudos-ms")
include("kudos-ms:kudos-ms-captcha")
findProject(":kudos-ms:kudos-ms-captcha")?.name = "kudos-ms-captcha"
include("kudos-ms:kudos-ms-captcha:kudos-ms-captcha-api-service")
findProject(":kudos-ms:kudos-ms-captcha:kudos-ms-captcha-api-service")?.name = "kudos-ms-captcha-api-service"
include("kudos-ms:kudos-ms-captcha:kudos-ms-captcha-api-view")
findProject(":kudos-ms:kudos-ms-captcha:kudos-ms-captcha-api-view")?.name = "kudos-ms-captcha-api-view"
include("kudos-ms:kudos-ms-captcha:kudos-ms-captcha-client")
findProject(":kudos-ms:kudos-ms-captcha:kudos-ms-captcha-client")?.name = "kudos-ms-captcha-client"
include("kudos-ms:kudos-ms-captcha:kudos-ms-captcha-common")
findProject(":kudos-ms:kudos-ms-captcha:kudos-ms-captcha-common")?.name = "kudos-ms-captcha-common"
include("kudos-ms:kudos-ms-captcha:kudos-ms-captcha-service")
findProject(":kudos-ms:kudos-ms-captcha:kudos-ms-captcha-service")?.name = "kudos-ms-captcha-service"
include("kudos-ms:kudos-ms-fserver")
findProject(":kudos-ms:kudos-ms-fserver")?.name = "kudos-ms-fserver"
include("kudos-ms:kudos-ms-fserver:kudos-ms-fserver-api-view")
findProject(":kudos-ms:kudos-ms-fserver:kudos-ms-fserver-api-view")?.name = "kudos-ms-fserver-api-view"
include("kudos-ms:kudos-ms-log")
findProject(":kudos-ms:kudos-ms-log")?.name = "kudos-ms-log"
include("kudos-ms:kudos-ms-log:kudos-ms-log-api-service")
findProject(":kudos-ms:kudos-ms-log:kudos-ms-log-api-service")?.name = "kudos-ms-log-api-service"
include("kudos-ms:kudos-ms-log:kudos-ms-log-api-view")
findProject(":kudos-ms:kudos-ms-log:kudos-ms-log-api-view")?.name = "kudos-ms-log-api-view"
include("kudos-ms:kudos-ms-log:kudos-ms-log-service")
findProject(":kudos-ms:kudos-ms-log:kudos-ms-log-service")?.name = "kudos-ms-log-service"
include("kudos-ms:kudos-ms-notice")
findProject(":kudos-ms:kudos-ms-notice")?.name = "kudos-ms-notice"
include("kudos-ms:kudos-ms-notice:kudos-ms-notice-api-service")
findProject(":kudos-ms:kudos-ms-notice:kudos-ms-notice-api-service")?.name = "kudos-ms-notice-api-service"
include("kudos-ms:kudos-ms-notice:kudos-ms-notice-api-view")
findProject(":kudos-ms:kudos-ms-notice:kudos-ms-notice-api-view")?.name = "kudos-ms-notice-api-view"
include("kudos-ms:kudos-ms-notice:kudos-ms-notice-client")
findProject(":kudos-ms:kudos-ms-notice:kudos-ms-notice-client")?.name = "kudos-ms-notice-client"
include("kudos-ms:kudos-ms-notice:kudos-ms-notice-common")
findProject(":kudos-ms:kudos-ms-notice:kudos-ms-notice-common")?.name = "kudos-ms-notice-common"
include("kudos-ms:kudos-ms-notice:kudos-ms-notice-service")
findProject(":kudos-ms:kudos-ms-notice:kudos-ms-notice-service")?.name = "kudos-ms-notice-service"
include("kudos-ms:kudos-ms-sys")
findProject(":kudos-ms:kudos-ms-sys")?.name = "kudos-ms-sys"
include("kudos-ms:kudos-ms-sys:kudos-ms-sys-api-service")
findProject(":kudos-ms:kudos-ms-sys:kudos-ms-sys-api-service")?.name = "kudos-ms-sys-api-service"
include("kudos-ms:kudos-ms-sys:kudos-ms-sys-api-view")
findProject(":kudos-ms:kudos-ms-sys:kudos-ms-sys-api-view")?.name = "kudos-ms-sys-api-view"
include("kudos-ms:kudos-ms-sys:kudos-ms-sys-client")
findProject(":kudos-ms:kudos-ms-sys:kudos-ms-sys-client")?.name = "kudos-ms-sys-client"
include("kudos-ms:kudos-ms-sys:kudos-ms-sys-common")
findProject(":kudos-ms:kudos-ms-sys:kudos-ms-sys-common")?.name = "kudos-ms-sys-common"
include("kudos-ms:kudos-ms-sys:kudos-ms-sys-service")
findProject(":kudos-ms:kudos-ms-sys:kudos-ms-sys-service")?.name = "kudos-ms-sys-service"
include("kudos-ms:kudos-ms-user")
findProject(":kudos-ms:kudos-ms-user")?.name = "kudos-ms-user"
include("kudos-ms:kudos-ms-user:kudos-ms-user-api-service")
findProject(":kudos-ms:kudos-ms-user:kudos-ms-user-api-service")?.name = "kudos-ms-user-api-service"
include("kudos-ms:kudos-ms-user:kudos-ms-user-api-view")
findProject(":kudos-ms:kudos-ms-user:kudos-ms-user-api-view")?.name = "kudos-ms-user-api-view"
include("kudos-ms:kudos-ms-user:kudos-ms-user-client")
findProject(":kudos-ms:kudos-ms-user:kudos-ms-user-client")?.name = "kudos-ms-user-client"
include("kudos-ms:kudos-ms-user:kudos-ms-user-common")
findProject(":kudos-ms:kudos-ms-user:kudos-ms-user-common")?.name = "kudos-ms-user-common"
include("kudos-ms:kudos-ms-user:kudos-ms-user-service")
findProject(":kudos-ms:kudos-ms-user:kudos-ms-user-service")?.name = "kudos-ms-user-service"
include("kudos-ms:kudos-ms-workflow")
findProject(":kudos-ms:kudos-ms-workflow")?.name = "kudos-ms-workflow"
include("kudos-ms:kudos-ms-workflow:kudos-ms-workflow-api-view")
findProject(":kudos-ms:kudos-ms-workflow:kudos-ms-workflow-api-view")?.name = "kudos-ms-workflow-api-view"
include("kudos-ms:kudos-ms-workflow:kudos-ms-workflow-api-service")
findProject(":kudos-ms:kudos-ms-workflow:kudos-ms-workflow-api-service")?.name = "kudos-ms-workflow-api-service"
include("kudos-ms:kudos-ms-workflow:kudos-ms-workflow-api-view:kudos-ms-workflow-api-service")
findProject(":kudos-ms:kudos-ms-workflow:kudos-ms-workflow-api-view:kudos-ms-workflow-api-service")?.name = "kudos-ms-workflow-api-service"
include("kudos-ms:kudos-ms-workflow:kudos-ms-workflow-client")
findProject(":kudos-ms:kudos-ms-workflow:kudos-ms-workflow-client")?.name = "kudos-ms-workflow-client"
include("kudos-ms:kudos-ms-workflow:kudos-ms-workflow-common")
findProject(":kudos-ms:kudos-ms-workflow:kudos-ms-workflow-common")?.name = "kudos-ms-workflow-common"
include("kudos-ms:kudos-ms-workflow:kudos-ms-workflow-service")
findProject(":kudos-ms:kudos-ms-workflow:kudos-ms-workflow-service")?.name = "kudos-ms-workflow-service"


include("kudos-test")
include("kudos-test:kudos-test-common")
findProject(":kudos-test:kudos-test-common")?.name = "kudos-test-common"


include("kudos-tools")
