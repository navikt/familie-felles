package no.nav.familie.webflux.builder

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientFilterFunction
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.LocalDate

@EnableAutoConfiguration
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [WebClientConfig::class, WebClientMetricConfig::class])
@TestPropertySource(properties = ["application.name=test"])
internal class WebClientConfigTest {

    @Autowired
    @Qualifier(FAMILIE_WEB_CLIENT_BUILDER)
    lateinit var familieWebClientBuilder: WebClient.Builder

    @Autowired
    lateinit var webClientBuilder: WebClient.Builder

    data class TestDto(val dato: LocalDate = LocalDate.of(2020, 1, 1))

    @Test
    internal fun `familieWebClient skal ikke inneholde metricsFilter`() {
        var finnes = false
        familieWebClientBuilder.filters { finnes = it.any { filter -> filter is MetricsWebClientFilterFunction } }
        assertThat(finnes).isFalse
    }

    @Test
    internal fun `webClient skal inneholde metricsFilter`() {
        var finnes = false
        webClientBuilder.filters { finnes = it.any { filter -> filter is MetricsWebClientFilterFunction } }
        assertThat(finnes).isTrue
    }

    @Test
    internal fun `skal skrive dato som iso-string`() {
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.ok()))
        val build = familieWebClientBuilder.build()
        build
            .post()
            .uri("http://localhost:${wiremockServerItem.port()}")
            .bodyValue(TestDto())
            .retrieve()
            .bodyToMono<String>()
            .block()

        wiremockServerItem.verify(
            postRequestedFor(WireMock.anyUrl())
                .withRequestBody(WireMock.equalToJson("""{"dato" : "2020-01-01"} """))
        )
    }

    @Test
    internal fun `skal kunne motta en stor fil`() {
        val fil = this::class.java.classLoader.getResource("dummy/image_large.jpg")!!.readText()
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.ok().withBody(fil)))
        val build = familieWebClientBuilder.build()
        build
            .post()
            .uri("http://localhost:${wiremockServerItem.port()}")
            .bodyValue(TestDto())
            .retrieve()
            .bodyToMono<String>()
            .block()

        wiremockServerItem.verify(postRequestedFor(WireMock.anyUrl()))
    }

    @Test
    internal fun `skal kaste feil hvis endepunkt ikke finnes`() {
        wiremockServerItem.stubFor(WireMock.post(WireMock.anyUrl()).willReturn(WireMock.notFound()))
        val build = familieWebClientBuilder.build()
        assertThatThrownBy {
            build
                .post()
                .uri("http://localhost:${wiremockServerItem.port()}")
                .bodyValue(TestDto())
                .retrieve()
                .bodyToMono<String>()
                .block()
        }.isInstanceOf(WebClientResponseException.NotFound::class.java)
    }

    companion object {

        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wiremockServerItem.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServerItem.resetAll()
    }
}
