package no.nav.familie.saf.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.felles.klient.KlientTokenProvider
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.saf.SafException
import no.nav.familie.saf.SafOboKlient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.create
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class SafKlientAutoConfigurationTest {
    private val url = URI.create("https://saf.test")
    private val scope = "api://saf.test.app/.default"
    private val eksplisitteProperties =
        arrayOf("familie.felles.klient.saf.url=$url", "familie.felles.klient.saf.scope=$scope")

    @Test
    fun `disabled krever verken konfigurasjon eller tokenprovider`() {
        // Arrange
        val runner = runner(mockFactory(), "familie.felles.klient.saf.enabled=false")

        // Act
        runner
            .run { context ->
                // Assert
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(SafOboKlient::class.java)).isEmpty()
                assertThat(context.getBeansOfType(SafKlientProperties::class.java)).isEmpty()
            }
    }

    @Test
    fun `uten tokenprovider opprettes ingen klient`() {
        // Arrange
        val factory = mockFactory()
        val runner =
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SafKlientAutoConfiguration::class.java))

        // Act
        runner
            .run { context ->
                // Assert
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(SafOboKlient::class.java)).isEmpty()
                verify(exactly = 0) { factory.lagOboRestKlient(any(), any()) }
            }
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "preprod|api://dev-fss.teamdokumenthandtering.saf/.default|https://saf-q2.dev-fss-pub.nais.io",
            "prod|api://prod-fss.teamdokumenthandtering.saf/.default|https://saf.prod-fss-pub.nais.io",
        ],
    )
    fun `miljø gir riktige defaults og bare OBO`(
        miljø: String,
        forventetScope: String,
        forventetUrl: String,
    ) {
        // Arrange
        val factory = mockFactory()
        val runner = runner(factory, "familie.felles.klient.miljo=$miljø")

        // Act
        runner
            .run { context ->
                val builder = context.getBean<SafOboKlient>().restClient.mutate()
                val server = MockRestServiceServer.bindTo(builder).build()

                server
                    .expect(requestTo("$forventetUrl/graphql"))
                    .andRespond(withSuccess("""{"data":"svar"}""", APPLICATION_JSON))

                val svar =
                    SafOboKlient(URI.create(forventetUrl), builder.build())
                        .hent<String>("query { ping }", emptyMap())

                // Assert
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(SafOboKlient::class.java)).hasSize(1)
                assertThat(context.getBeanNamesForType(SafOboKlient::class.java)).containsExactly("safOboKlient")

                assertThat(svar).isEqualTo("svar")
                server.verify()

                verify(exactly = 1) { factory.lagOboRestKlient(forventetScope, any()) }
                verify(exactly = 0) { factory.lagHybridRestKlient(any(), any()) }
                verify(exactly = 0) { factory.lagMaskinTilMaskinRestKlient(any()) }
            }
    }

    @Test
    fun `eksplisitte verdier overstyrer miljø og binder timeouter`() {
        // Arrange
        val factory = mockFactory()
        val runner =
            runner(
                factory,
                "familie.felles.klient.miljo=prod",
                *eksplisitteProperties,
                "familie.felles.klient.saf.connect-timeout=3s",
                "familie.felles.klient.saf.read-timeout=7s",
            )

        // Act
        runner.run { context ->
            // Assert
            assertThat(context.startupFailure).isNull()
            with(context.getBean<SafKlientProperties>()) {
                assertThat(hentUrl(null)).isEqualTo(url.toString())
                assertThat(hentScope(null)).isEqualTo(scope)
                assertThat(connectTimeout).isEqualTo(Duration.ofSeconds(3))
                assertThat(readTimeout).isEqualTo(Duration.ofSeconds(7))
            }
            verify(exactly = 1) { factory.lagOboRestKlient(scope, any()) }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["", "familie.felles.klient.saf.url=https://saf.test", "familie.felles.klient.saf.scope=api://saf.test.app/.default"],
    )
    fun `manglende miljø eller eksplisitt konfigurasjon stopper oppstart`(property: String) {
        // Arrange
        val runner = runner(mockFactory(), property)

        // Act
        runner
            .run { context ->
                // Assert
                assertThat(context.startupFailure).isNotNull()
            }
    }

    @Test
    fun `egen klient erstatter autokonfigurert klient`() {
        // Arrange
        val factory = mockFactory()
        val egenKlient = SafOboKlient(url, create())
        val runner =
            runner(factory, *eksplisitteProperties)
                .withBean("egenKlient", SafOboKlient::class.java, { egenKlient })

        // Act
        runner
            .run { context ->
                // Assert
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(SafOboKlient::class.java).values).containsExactly(egenKlient)
                assertThat(context.getBeanNamesForType(SafOboKlient::class.java)).containsExactly("egenKlient")
                verify(exactly = 0) { factory.lagOboRestKlient(any(), any()) }
            }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `retry properties brukes av autokonfigurert klient`(retryAktivert: Boolean) {
        // Arrange
        val antallKall = AtomicInteger()
        val restClient =
            RestClient
                .builder()
                .requestInterceptor { _, _, _ ->
                    antallKall.incrementAndGet()
                    MockClientHttpResponse(ByteArray(0), HttpStatus.SERVICE_UNAVAILABLE)
                }.build()

        val runner =
            runner(
                mockFactory(restClient),
                *eksplisitteProperties,
                "familie.felles.klient.saf.retry.enabled=$retryAktivert",
                "familie.felles.klient.saf.retry.max-retries=1",
                "familie.felles.klient.saf.retry.delay=1ms",
            )

        // Act
        runner.run { context ->
            val feil =
                assertThrows<SafException> {
                    context.getBean<SafOboKlient>().hent<String>("query { ping }", emptyMap())
                }

            // Assert
            assertThat(feil.httpStatus).isEqualTo(503)
            assertThat(antallKall.get()).isEqualTo(if (retryAktivert) 2 else 1)
        }
    }

    private fun runner(
        factory: EntraIDRestClientFactory,
        vararg properties: String,
    ) = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SafKlientAutoConfiguration::class.java))
        .withPropertyValues(
            "NAIS_TOKEN_ENDPOINT=https://token.test/token",
            "NAIS_TOKEN_EXCHANGE_ENDPOINT=https://token.test/exchange",
            "application.name=test-app",
            *properties,
        ).withBean(EntraIDRestClientFactory::class.java, { factory })
        .withBean(KlientTokenProvider::class.java, { KlientTokenProvider { "token" } })

    private fun mockFactory(restClient: RestClient = create()) =
        mockk<EntraIDRestClientFactory> {
            every { lagOboRestKlient(any(), any()) } returns restClient
        }
}
