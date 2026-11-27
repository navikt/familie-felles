package no.nav.familie.saf

import no.nav.familie.felles.klient.retry.KlientRetryProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.ExpectedCount.twice
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class SafRetryTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val klient = SafOboKlient(URI.create(BASE_URL), builder.build(), retry())

    @AfterEach
    fun verifiserKall() {
        server.verify()
    }

    @Nested
    inner class Hent {
        @ParameterizedTest
        @ValueSource(ints = [502, 503, 504])
        fun `midlertidige http-feil fra graphql gir nytt forsøk`(status: Int) {
            // Arrange
            val antallKall = AtomicInteger()
            server
                .expect(twice(), requestTo(GRAPHQL_URL))
                .andRespond {
                    if (antallKall.incrementAndGet() == 1) svar(status) else svar(200)
                }

            // Act
            val svar = klient.hent<String>(QUERY, emptyMap())

            // Assert
            assertThat(svar).isEqualTo("data")
            assertThat(antallKall.get()).isEqualTo(2)
        }

        @ParameterizedTest
        @ValueSource(ints = [404, 500])
        fun `andre http-feil fra graphql gir ett forsøk`(status: Int) {
            // Arrange
            val antallKall = AtomicInteger()
            server.expect(once(), requestTo(GRAPHQL_URL)).andRespond {
                antallKall.incrementAndGet()
                svar(status)
            }

            // Act
            val feil = assertThrows<SafException> { klient.hent<String>(QUERY, emptyMap()) }

            // Assert
            assertThat(feil.httpStatus).isEqualTo(status)
            assertThat(antallKall.get()).isEqualTo(1)
        }
    }

    @Nested
    inner class HentDokument {
        @Test
        fun `dokument-get retryer også ved midlertidig http-feil`() {
            // Arrange
            val antallKall = AtomicInteger()
            val klientMedRetry = SafOboKlient(URI.create(BASE_URL), builder.build(), retry())
            server
                .expect(twice(), requestTo(DOKUMENT_URL))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, MediaType.ALL_VALUE))
                .andRespond {
                    if (antallKall.incrementAndGet() == 1) {
                        svar(503)
                    } else {
                        MockClientHttpResponse(
                            byteArrayOf(6, 7),
                            HttpStatus.OK,
                        )
                    }
                }

            // Act
            val dokument = klientMedRetry.hentDokument(JOURNAL_POST_ID, DOKUMENT_INFO_ID, SafVariantFormat.ORIGINAL)

            // Assert
            assertThat(dokument).containsExactly(6, 7)
            assertThat(antallKall.get()).isEqualTo(2)
        }
    }

    private fun retry() = KlientRetryProperties(maxRetries = 1, delay = Duration.ofMillis(1))

    private fun svar(
        status: Int,
        body: String = """{"data":"data"}""",
    ) = MockClientHttpResponse(body.toByteArray(), HttpStatusCode.valueOf(status)).apply {
        headers.contentType = MediaType.APPLICATION_JSON
    }

    companion object {
        private const val BASE_URL = "https://saf.test"

        private const val GRAPHQL_URL = "$BASE_URL/graphql"
        private const val QUERY = "query { ping }"

        private const val JOURNAL_POST_ID = "12345678910"
        private const val DOKUMENT_INFO_ID = "10987654321"
        private const val DOKUMENT_URL = "$BASE_URL/rest/hentdokument/$JOURNAL_POST_ID/$DOKUMENT_INFO_ID/ORIGINAL"
    }
}
