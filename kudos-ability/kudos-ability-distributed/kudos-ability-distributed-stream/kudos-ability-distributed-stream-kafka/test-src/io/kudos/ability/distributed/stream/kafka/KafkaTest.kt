package io.kudos.ability.distributed.stream.kafka

import org.soul.ability.distributed.stream.kafka.main.IKafkaMainService

@EnableKudosTest
@SpringBootTest
@EnableFeignClients
@ComponentScan(
    basePackages = ["org.soul.ability.distributed.stream.kafka.main"
    ]
)
@org.springframework.context.annotation.PropertySource(
    value = ["classpath:application-kafka-main.yml"
    ], factory = SoulPropertySourceFactory::class
)
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
@org.testcontainers.junit.jupiter.Testcontainers(disabledWithoutDocker = true)
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
class KafkaTest {
    @Autowired
    private val mainService: IKafkaMainService? = null
    private val kafkaContainer: GenericContainer<*> = TestContainerKafka.getContainer()
    private var producerApplication: ConfigurableApplicationContext? = null

    @BeforeAll
    @kotlin.Throws(java.lang.InterruptedException::class)
    fun setUp() {
        val url: kotlin.String = "jdbc:postgresql://%s:%s/%s".formatted(
            IpTool.getLocalIp(),
            TestContainerPostgres.PORT,
            TestContainerPostgres.DATABASE
        )
        val args: kotlin.Array<kotlin.String?> = kotlin.arrayOf<kotlin.String>(
            "--spring.datasource.dynamic.datasource.postgres.url=" + url,
            "--soul.ability.distributed.stream.mq-config.kafka-brokers=" + kafkaContainer.getHost() + ":" + kafkaContainer.getFirstMappedPort()
        )
        producerApplication = SpringApplication.run(KafkaProducerApplication::class.java, *args)
    }

    @AfterAll
    fun tearDown() {
        if (producerApplication != null) {
            producerApplication.close()
        }
    }

    /**
     * 发送与接收测试
     */
    @org.junit.jupiter.api.Test
    fun sendAndReceiveMessageTest() {
        val task: java.util.concurrent.Callable<kotlin.String?> =
            object : java.util.concurrent.Callable<kotlin.String?> {
                @kotlin.Throws(java.lang.Exception::class)
                override fun call(): kotlin.String {
                    return mainService.sendAndReceiveMessage()
                }
            }
        val future: java.util.concurrent.Future<kotlin.String?> =
            KafkaTest.Companion.EXECUTOR.submit<kotlin.String?>(task)
        try {
            future.get(30, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: java.lang.Exception) {
            throw java.lang.RuntimeException("等待时间超过30秒，mq接收异常")
        } finally {
            future.cancel(true)
        }
    }

    /**
     * 消化信息异常测试 StreamException
     */
    @org.junit.jupiter.api.Test
    fun streamExceptionTest() {
        val task: java.util.concurrent.Callable<kotlin.String?> =
            object : java.util.concurrent.Callable<kotlin.String?> {
                @kotlin.Throws(java.lang.Exception::class)
                override fun call(): kotlin.String {
                    return mainService.errorMessage()
                }
            }
        val future: java.util.concurrent.Future<kotlin.String?> =
            KafkaTest.Companion.EXECUTOR.submit<kotlin.String?>(task)
        try {
            future.get(30, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: java.lang.Exception) {
            throw java.lang.RuntimeException("等待时间超过30秒，mq接收异常")
        } finally {
            future.cancel(true)
        }
    }

    companion object {
        @DynamicPropertySource
        @kotlin.Throws(java.lang.InterruptedException::class)
        private fun registerProperties(registry: DynamicPropertyRegistry?) {
            TestContainerPostgres.start(registry)
            TestContainerNacos.start(registry)
            TestContainerKafka.start(registry)
        }

        private val EXECUTOR: ExecutorService = Executors.newFixedThreadPool(3)
    }
}
