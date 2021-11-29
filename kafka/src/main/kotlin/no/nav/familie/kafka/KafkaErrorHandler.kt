package no.nav.familie.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.listener.ContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.scheduling.TaskScheduler
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


/**
 * Hindrer spam i loggene ved gjentakende feil
 */
class KafkaErrorHandler(private val taskScheduler: TaskScheduler) : ContainerStoppingErrorHandler() {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    private val counter = AtomicInteger(0)
    private val lastError = AtomicLong(0)

    override fun handle(e: Exception,
                        records: List<ConsumerRecord<*, *>>,
                        consumer: Consumer<*, *>,
                        container: MessageListenerContainer
    ) {
        if (records.isEmpty()) {
            logger.error("Feil ved konsumering av melding. Ingen records. ${consumer.subscription()}", e)
            scheduleRestart(e, records, consumer, container, "Ukjent topic")
        } else {
            records.first().run {
                logger.error(
                        "Feil ved konsumering av melding fra ${this.topic()}. id ${this.key()}, " +
                        "offset: ${this.offset()}, partition: ${this.partition()}"
                )
                secureLogger.error("${this.topic()} - Problemer med prosessering av $records", e)
                scheduleRestart(e, records, consumer, container, this.topic())
            }
        }
    }

    private fun scheduleRestart(e: Exception,
                                records: List<ConsumerRecord<*, *>>,
                                consumer: Consumer<*, *>,
                                container: MessageListenerContainer,
                                topic: String) {
        val now = System.currentTimeMillis()
        if (now - lastError.getAndSet(now) > COUNTER_RESET_TIME) {
            counter.set(0)
        }
        val numErrors = counter.incrementAndGet()
        val stopTime = if (numErrors > SLOW_ERROR_COUNT) LONG else SHORT * numErrors
        taskScheduler.schedule( {
                                    try {
                                        logger.warn("Starter kafka container for {}", topic)
                                        container.start()
                                    } catch (exception: Exception) {
                                        logger.error("Feil oppstod ved venting og oppstart av kafka container", exception)
                                    }
                                },
                                Instant.ofEpochMilli(now + stopTime ))
        logger.warn("Stopper kafka container for {} i {}", topic, Duration.ofMillis(stopTime))
        super.handle(Exception("Sjekk securelogs for mer info - ${e::class.java.simpleName}"), records, consumer, container)
    }

    companion object {
        private val LONG = Duration.ofHours(3).toMillis()
        private val SHORT = Duration.ofSeconds(20).toMillis()
        private const val SLOW_ERROR_COUNT = 10
        private val COUNTER_RESET_TIME =
                SHORT * SLOW_ERROR_COUNT * 2
    }

}
