package no.nav.familie.tilgangsmaskin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilgangsmaskinDtoTest {
    @Test
    fun `toString på BrukerIdOgRegelsettDto skal ikke inneholde personidenten`() {
        // Arrange
        val dto = BrukerIdOgRegelsettDto(brukerId = PERSONIDENT, type = Regeltype.KJERNE_REGELTYPE)

        // Act
        val tekst = dto.toString()

        // Assert
        assertThat(tekst).doesNotContain(PERSONIDENT)
        assertThat(tekst).contains("KJERNE_REGELTYPE")
    }

    @Test
    fun `toString på TilgangsmaskinBulkResultatDto skal ikke inneholde personidenten`() {
        // Arrange
        val dto = TilgangsmaskinBulkResultatDto(brukerId = PERSONIDENT, status = 403)

        // Act
        val tekst = dto.toString()

        // Assert
        assertThat(tekst).doesNotContain(PERSONIDENT)
        assertThat(tekst).contains("403")
    }

    @Test
    fun `toString på TilgangsmaskinResultat skal ikke inneholde personidenten`() {
        // Arrange
        val resultat =
            TilgangsmaskinResultat(
                personIdent = PERSONIDENT,
                harTilgang = false,
                httpStatus = 403,
                avvisningskode = Avvisningskode.AVVIST_SKJERMING,
                traceId = "en-trace-id",
            )

        // Act
        val tekst = resultat.toString()

        // Assert
        assertThat(tekst).doesNotContain(PERSONIDENT)
        assertThat(tekst).contains("AVVIST_SKJERMING", "en-trace-id")
    }

    @Test
    fun `toString på TilgangsmaskinBulkResponsDto skal ikke inneholde personidenten`() {
        // Arrange
        val respons =
            TilgangsmaskinBulkResponsDto(
                resultater = listOf(TilgangsmaskinBulkResultatDto(brukerId = PERSONIDENT, status = 204)),
                ansattId = "Z999999",
            )

        // Act
        val tekst = respons.toString()

        // Assert
        assertThat(tekst).doesNotContain(PERSONIDENT)
    }

    @Test
    fun `maskering skal beholde lengden slik at man ser at verdien var satt`() {
        // Arrange
        val dto = BrukerIdOgRegelsettDto(brukerId = PERSONIDENT, type = Regeltype.KJERNE_REGELTYPE)

        // Act
        val tekst = dto.toString()

        // Assert
        assertThat(tekst).contains("*".repeat(PERSONIDENT.length))
    }

    companion object {
        private const val PERSONIDENT = "12345678910"
    }
}
