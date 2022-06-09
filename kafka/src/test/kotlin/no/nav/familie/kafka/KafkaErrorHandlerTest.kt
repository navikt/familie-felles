package no.nav.familie.kafka

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.impl.annotations.MockK
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaErrorHandlerTest {

    @MockK(relaxed = true)
    lateinit var container: MessageListenerContainer

    @MockK(relaxed = true)
    lateinit var consumer: Consumer<*, *>

    private val taskScheduler: TaskScheduler = DefaultManagedTaskScheduler()

    private val errorHandler: KafkaErrorHandler = KafkaErrorHandler(taskScheduler)

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        clearAllMocks()
    }

    @Test
    fun `skal stoppe container hvis man mottar feil med en tom liste med records`() {
        assertThatThrownBy { errorHandler.handle(RuntimeException("Feil i test"), emptyList(), consumer, container) }
            .hasMessageNotContaining("Feil i test")
            .hasMessageContaining("Sjekk securelogs for mer info")
            .hasCauseExactlyInstanceOf(Exception::class.java)
    }

    @Test
    fun `skal stoppe container hvis man mottar feil med en liste med records`() {
        val consumerRecord = ConsumerRecord("topic", 1, 1, 1, "record")
        assertThatThrownBy { errorHandler.handle(RuntimeException("Feil i test"), listOf(consumerRecord), consumer, container) }
            .hasMessageNotContaining("Feil i test")
            .hasMessageContaining("Sjekk securelogs for mer info")
            .hasCauseExactlyInstanceOf(Exception::class.java)
    }
}
