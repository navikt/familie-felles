package no.nav.familie.felles.klient.retry

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.util.backoff.BackOffExecution
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class KlientRetryTest {
    @Test
    fun `suksess returnerer samme verdi`() {
        val forventet = Any()

        assertThat(KlientRetry(retryKonfig()).utfør { forventet }).isSameAs(forventet)
    }

    @ParameterizedTest
    @ValueSource(ints = [502, 503, 504])
    fun `midlertidige HTTP-feil gir nytt forsøk`(status: Int) {
        val antallKall = AtomicInteger()

        val resultat =
            KlientRetry(retryKonfig()).utfør {
                if (antallKall.incrementAndGet() == 1) throw serverfeil(status)
                "ferdig"
            }

        assertThat(resultat).isEqualTo("ferdig")
        assertThat(antallKall.get()).isEqualTo(2)
    }

    @Test
    fun `500 transportfeil og 503 i årsakskjeden gir ikke nytt forsøk`() {
        listOf(
            serverfeil(500),
            ResourceAccessException("Transportfeil", SocketTimeoutException("Read timed out")),
            IllegalStateException("Innpakket feil", serverfeil(503)),
        ).forEach { feil ->
            val antallKall = AtomicInteger()

            assertThatThrownBy {
                KlientRetry(retryKonfig()).utfør {
                    antallKall.incrementAndGet()
                    throw feil
                }
            }.isSameAs(feil)

            assertThat(antallKall.get()).isEqualTo(1)
        }
    }

    @ParameterizedTest
    @ValueSource(longs = [0, 1, 5])
    fun `antall kall er første kall pluss max-retries og sluttfeilen får ikke suppressed`(maxRetries: Long) {
        val feil = serverfeil(503)
        val antallKall = AtomicInteger()

        assertThatThrownBy {
            KlientRetry(retryKonfig(maxRetries = maxRetries)).utfør {
                antallKall.incrementAndGet()
                throw feil
            }
        }.isSameAs(feil)

        assertThat(antallKall.get()).isEqualTo((maxRetries + 1).toInt())
        assertThat(feil.suppressed).isEmpty()
    }

    @Test
    fun `deaktivert retry gjør bare ett kall`() {
        val antallKall = AtomicInteger()

        assertThatThrownBy {
            KlientRetry(retryKonfig(enabled = false)).utfør {
                antallKall.incrementAndGet()
                throw serverfeil(503)
            }
        }.isInstanceOf(HttpServerErrorException::class.java)

        assertThat(antallKall.get()).isEqualTo(1)
    }

    @Test
    fun `ventetiden er konstant med jitter på inntil en halv delay`() {
        val backoff =
            KlientRetry(retryKonfig(maxRetries = 2, delay = Duration.ofMillis(100)))
                .retryTemplate.retryPolicy.backOff
                .start()

        assertThat(backoff.nextBackOff()).isBetween(100L, 150L)
        assertThat(backoff.nextBackOff()).isBetween(100L, 150L)
        assertThat(backoff.nextBackOff()).isEqualTo(BackOffExecution.STOP)
    }

    @Test
    fun `avbrudd under venting stopper forsøk og kaster siste feil med flagget satt`() {
        val førsteKallUtført = CountDownLatch(1)
        val antallKall = AtomicInteger()
        val sisteFeil = serverfeil(503)
        val fangetFeil = AtomicReference<Throwable?>()
        val avbruddsflaggSatt = AtomicBoolean()
        val retry = KlientRetry(retryKonfig(maxRetries = 2, delay = Duration.ofSeconds(1)))
        val tråd =
            Thread {
                try {
                    retry.utfør {
                        antallKall.incrementAndGet()
                        førsteKallUtført.countDown()
                        throw sisteFeil
                    }
                } catch (feil: Throwable) {
                    fangetFeil.set(feil)
                    avbruddsflaggSatt.set(Thread.currentThread().isInterrupted)
                }
            }

        tråd.start()
        try {
            assertThat(førsteKallUtført.await(5, TimeUnit.SECONDS)).isTrue()
            tråd.interrupt()
            tråd.join(TimeUnit.SECONDS.toMillis(5))

            assertThat(tråd.isAlive).isFalse()
            assertThat(antallKall.get()).isEqualTo(1)
            assertThat(fangetFeil.get()).isSameAs(sisteFeil)
            assertThat(avbruddsflaggSatt.get()).isTrue()
        } finally {
            if (tråd.isAlive) {
                tråd.interrupt()
                tråd.join(TimeUnit.SECONDS.toMillis(5))
            }
        }
    }

    @Test
    fun `allerede avbrutt tråd gjør første kall men ingen nye forsøk`() {
        val antallKall = AtomicInteger()
        val feil = serverfeil(503)
        Thread.currentThread().interrupt()

        try {
            assertThatThrownBy {
                KlientRetry(retryKonfig(maxRetries = 2)).utfør {
                    antallKall.incrementAndGet()
                    throw feil
                }
            }.isSameAs(feil)

            assertThat(antallKall.get()).isEqualTo(1)
            assertThat(Thread.currentThread().isInterrupted).isTrue()
        } finally {
            Thread.interrupted()
        }
    }

    private fun retryKonfig(
        enabled: Boolean = true,
        maxRetries: Long = 1,
        delay: Duration = Duration.ofMillis(1),
    ) = KlientRetryProperties(enabled, maxRetries, delay)

    private fun serverfeil(status: Int) =
        HttpServerErrorException.create(
            HttpStatusCode.valueOf(status),
            "Serverfeil",
            HttpHeaders.EMPTY,
            byteArrayOf(),
            StandardCharsets.UTF_8,
        )
}
