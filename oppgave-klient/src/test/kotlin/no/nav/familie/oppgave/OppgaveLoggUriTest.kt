package no.nav.familie.oppgave

import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.oppgave.modell.FinnOppgaverRequest
import no.nav.familie.oppgave.modell.OpprettOppgaveRequest
import no.nav.familie.oppgave.modell.PatchOppgaveRequest
import no.nav.familie.oppgave.modell.Prioritet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.MDC
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class OppgaveLoggUriTest {
    @Test
    fun `logg-URI følger scheme host port og raw path for faktiske kall`() {
        // Arrange
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val klient = OppgaveMaskinKlient(URI.create("https://oppgave.test/"), builder.build())
        val faktiskeUrier = mutableListOf<URI>()
        server
            .expect(once()) { faktiskeUrier.add(it.uri) }
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(OPPGAVE_JSON))
        server
            .expect(once()) { faktiskeUrier.add(it.uri) }
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(EMPTY_SEARCH_JSON, MediaType.APPLICATION_JSON))
        server
            .expect(once()) { faktiskeUrier.add(it.uri) }
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))
        server
            .expect(once()) { faktiskeUrier.add(it.uri) }
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(withSuccess(OPPGAVE_JSON, MediaType.APPLICATION_JSON))

        // Act
        klient.opprettOppgave(
            OpprettOppgaveRequest(
                aktivDato = LocalDate.parse("2026-01-01"),
                oppgavetype = "TEST",
                prioritet = Prioritet.NORM,
                tema = "BID",
                uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                tildeltEnhetsnr = "0000",
            ),
        )
        klient.finnOppgaver(
            FinnOppgaverRequest(
                aktoerId = listOf("0000000000000"),
                bnr = listOf("syntetisk-bnr"),
                saksreferanse = listOf("syntetisk-sak"),
            ),
        )
        klient.hentOppgave(OPPGAVE_ID)
        klient.patchOppgave(OPPGAVE_ID, PatchOppgaveRequest(versjon = 1))

        // Assert
        server.verify()
        val loggUrier =
            listOf(klient.uriForLogging(), klient.uriForLogging(), klient.uriForLogging(OPPGAVE_ID), klient.uriForLogging(OPPGAVE_ID))
        assertThat(faktiskeUrier).hasSize(4)
        loggUrier.zip(faktiskeUrier).forEach { (loggUri, faktiskUri) ->
            assertThat(loggUri.scheme).isEqualTo(faktiskUri.scheme)
            assertThat(loggUri.host).isEqualTo(faktiskUri.host)
            assertThat(loggUri.port).isEqualTo(faktiskUri.port)
            assertThat(loggUri.rawPath).isEqualTo(faktiskUri.rawPath)
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://syntetisk-bruker:syntetisk-passord@[2001:db8::1]:8443/base/a%20b/?syntetisk-token=hemmelig#syntetisk-fragment",
            "https://syntetisk-bruker:syntetisk-passord@[2001:db8::1]:8443/base/a%20b?syntetisk-token=hemmelig#syntetisk-fragment",
        ],
    )
    fun `logg-URI fjerner userinfo query og fragment uten å endre faktisk søke-URI`(baseadresse: String) {
        // Arrange
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val klient =
            OppgaveMaskinKlient(
                URI.create(baseadresse),
                builder.build(),
            )
        lateinit var faktiskUri: URI
        server
            .expect(once()) { faktiskUri = it.uri }
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(EMPTY_SEARCH_JSON, MediaType.APPLICATION_JSON))
        val request =
            FinnOppgaverRequest(
                aktoerId = listOf("0000000000000"),
                bnr = listOf("syntetisk-bnr"),
                saksreferanse = listOf("syntetisk-sak"),
            )

        // Act
        klient.finnOppgaver(request)
        val loggUri = klient.uriForLogging()

        // Assert
        server.verify()
        assertThat(loggUri.scheme).isEqualTo(faktiskUri.scheme)
        assertThat(loggUri.host).isEqualTo(faktiskUri.host)
        assertThat(loggUri.port).isEqualTo(faktiskUri.port)
        assertThat(loggUri.rawPath).isEqualTo(faktiskUri.rawPath)
        assertThat(loggUri.rawUserInfo).isNull()
        assertThat(loggUri.rawQuery).isNull()
        assertThat(loggUri.rawFragment).isNull()
        assertThat(loggUri.toString())
            .doesNotContain(
                "syntetisk-bruker",
                "syntetisk-passord",
                "syntetisk-token",
                "syntetisk-fragment",
                "0000000000000",
                "syntetisk-bnr",
                "syntetisk-sak",
            )
        assertThat(faktiskUri.rawUserInfo).isEqualTo("syntetisk-bruker:syntetisk-passord")
        assertThat(faktiskUri.rawQuery)
            .contains("syntetisk-token=hemmelig", "aktoerId=0000000000000", "bnr=syntetisk-bnr", "saksreferanse=syntetisk-sak")
        assertThat(faktiskUri.rawFragment).isEqualTo("syntetisk-fragment")
    }

    private companion object {
        const val OPPGAVE_ID = 321L
        const val EMPTY_SEARCH_JSON = """{"antallTreffTotalt":0,"oppgaver":[]}"""
        const val OPPGAVE_JSON =
            """{"id":321,"tildeltEnhetsnr":"0000","tema":"BID","oppgavetype":"TEST","versjon":1,"prioritet":"NORM","status":"OPPRETTET","aktivDato":"2026-01-01"}"""
    }
}
