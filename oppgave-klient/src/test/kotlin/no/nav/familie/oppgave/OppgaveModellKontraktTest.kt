package no.nav.familie.oppgave

import no.nav.familie.oppgave.modell.Bruker
import no.nav.familie.oppgave.modell.Brukertype
import no.nav.familie.oppgave.modell.FinnOppgaverRequest
import no.nav.familie.oppgave.modell.Gruppe
import no.nav.familie.oppgave.modell.Ident
import no.nav.familie.oppgave.modell.MetadataKey
import no.nav.familie.oppgave.modell.Oppgave
import no.nav.familie.oppgave.modell.Oppgavestatus
import no.nav.familie.oppgave.modell.OpprettOppgaveRequest
import no.nav.familie.oppgave.modell.PatchFelt
import no.nav.familie.oppgave.modell.PatchOppgaveRequest
import no.nav.familie.oppgave.modell.Prioritet
import no.nav.familie.oppgave.modell.RegistrerKommentar
import no.nav.familie.oppgave.modell.SokOppgaverResponse
import no.nav.familie.oppgave.modell.Sorteringsfelt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID

class OppgaveModellKontraktTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `PATCH mappeId skiller mellom verdi null og utelatt`() {
        // Arrange
        val verdi = PatchOppgaveRequest(versjon = 4, mappeId = PatchFelt.Verdi(987L))
        val nullverdi = PatchOppgaveRequest(versjon = 4, mappeId = PatchFelt.Null)
        val utelatt = PatchOppgaveRequest(versjon = 4)

        // Act
        val verdiJson = mapper.readTree(mapper.writeValueAsString(verdi))
        val nullJson = mapper.readTree(mapper.writeValueAsString(nullverdi))
        val utelattJson = mapper.readTree(mapper.writeValueAsString(utelatt))

        // Assert
        assertThat(verdiJson["mappeId"].asLong()).isEqualTo(987L)
        assertThat(nullJson.has("mappeId")).isTrue()
        assertThat(nullJson["mappeId"].isNull).isTrue()
        assertThat(utelattJson.has("mappeId")).isFalse()
    }

    @Test
    fun `metadata bruker enum-navn som map keys ved lesing og skriving`() {
        // Arrange
        val json = """{"NORM_DATO":"2026-04-03","SOKNAD_ID":"syntetisk-soknad"}"""

        // Act
        val metadata: Map<MetadataKey, String> = mapper.readValue(json)
        val serialisert = mapper.readTree(mapper.writeValueAsString(metadata))

        // Assert
        assertThat(metadata).containsEntry(MetadataKey.NORM_DATO, "2026-04-03")
        assertThat(metadata).containsEntry(MetadataKey.SOKNAD_ID, "syntetisk-soknad")
        assertThat(serialisert.propertyNames().toList()).containsExactlyInAnyOrder("NORM_DATO", "SOKNAD_ID")
        assertThat(serialisert["NORM_DATO"].asString()).isEqualTo("2026-04-03")
    }

    @Test
    fun `leser response Ident Gruppe alias og ignorerer ukjente felter`() {
        // Arrange
        val json =
            """
            {
              "id": 321,
              "tildeltEnhetsnr": "1234",
              "tema": "BAR",
              "oppgavetype": "JFR",
              "versjon": 4,
              "prioritet": "NORM",
              "status": "OPPRETTET",
              "aktivDato": "2026-04-03",
              "bruker": {"ident": "syntetisk-bruker", "type": "PERSON", "ukjentBrukerfelt": true},
              "identer": [
                {"ident": "0000000000000", "gruppe": "AKTORID", "ukjentIdentfelt": true},
                {"ident": "00000000000", "gruppe": "FOLKEREGISTERIDENT"}
              ],
              "metadata": {"NORM_DATO": "2026-04-03", "SOKNAD_ID": "syntetisk-soknad"},
              "ukjentResponsfelt": true
            }
            """.trimIndent()

        // Act
        val oppgave: Oppgave = mapper.readValue(json)

        // Assert
        assertThat(oppgave.identer).containsExactly(
            Ident("0000000000000", Gruppe.AKTOERID),
            Ident("00000000000", Gruppe.FOLKEREGISTERIDENT),
        )
        assertThat(oppgave.bruker).isEqualTo(Bruker("syntetisk-bruker", Brukertype.PERSON))
        assertThat(oppgave.metadata).containsEntry(MetadataKey.NORM_DATO, "2026-04-03")
    }

    @Test
    fun `DTO toString inneholder ikke syntetisk sensitiv markør`() {
        // Arrange
        val markor = "syntetisk-markor-123"
        val metadata = mapOf(MetadataKey.NORM_DATO to markor)
        val kommentar = RegistrerKommentar(tekst = markor, automatiskGenerert = false)
        val bruker = Bruker(ident = markor, type = Brukertype.PERSON)
        val ident = Ident(ident = markor, gruppe = Gruppe.FOLKEREGISTERIDENT)
        val oppgave =
            Oppgave(
                id = 321,
                tildeltEnhetsnr = "1234",
                tema = "BAR",
                oppgavetype = "JFR",
                versjon = 4,
                prioritet = Prioritet.NORM,
                status = Oppgavestatus.OPPRETTET,
                aktivDato = LocalDate.of(2026, 4, 3),
                bruker = bruker,
                identer = listOf(ident),
                metadata = metadata,
                kommentar = kommentar,
            )
        val dtoTekster =
            listOf(
                OpprettOppgaveRequest(
                    aktivDato = LocalDate.of(2026, 4, 3),
                    oppgavetype = markor,
                    prioritet = Prioritet.NORM,
                    tema = markor,
                    uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                    personident = markor,
                    metadata = metadata,
                ).toString(),
                PatchOppgaveRequest(versjon = 4, beskrivelse = PatchFelt.Verdi(markor), mappeId = PatchFelt.Verdi(321L)).toString(),
                FinnOppgaverRequest(tema = listOf(markor), sorteringsfelt = Sorteringsfelt.FRIST).toString(),
                oppgave.toString(),
                bruker.toString(),
                ident.toString(),
                kommentar.toString(),
                SokOppgaverResponse(antallTreffTotalt = 1, oppgaver = listOf(oppgave)).toString(),
                PatchFelt.Verdi(markor).toString(),
            )

        // Act
        val dtoTekst = dtoTekster.joinToString("|")

        // Assert
        assertThat(dtoTekst).doesNotContain(markor)
    }
}
