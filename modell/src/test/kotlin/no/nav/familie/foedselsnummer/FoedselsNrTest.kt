package no.nav.familie.foedselsnummer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FoedselsNrTest {
    @Test
    fun kjoenn() {
        val mann = FoedselsNr("00000000191")
        val kvinne = FoedselsNr("00000000272")

        assertThat(mann.kjoenn).isEqualTo(Kjoenn.MANN)
        assertThat(kvinne.kjoenn).isEqualTo(Kjoenn.KVINNE)
    }

    @Test
    fun kontrollsifferSomInneholder0() {
        FoedselsNr("01010150740")
        FoedselsNr("01010150902")
    }

    @Test
    fun foedselsdato() {
        // 000-499 // 1900-1999
        testFdato("020200 000 69", 1900)
        testFdato("020200 499 47", 1900)
        testFdato("010299 000 31", 1999)
        testFdato("020299 499 59", 1999)

        // 500-999, overlapper alt under // 2000-2039
        testFdato("010200 500 43", 2000) // 500: overlapp 1854-1899
        testFdato("010200 749 29", 2000) // 749: overlapp 1854-1899
        testFdato("020200 900 33", 2000) // 900: overlapp 1940-1999
        testFdato("010200 999 21", 2000) // 999: overlapp 1940-1999

        testFdato("010239 500 96", 2039) // 500: overlapp 1854-1899
        testFdato("010239 749 71", 2039) // 749: overlapp 1854-1899
        testFdato("010239 900 47", 2039) // 900: overlapp 1940-1999
        testFdato("010239 999 74", 2039) // 999: overlapp 1940-1999

        // 500-749, overlapp // 1854-1899
        testFdato("010254 500 58", 1854) // 500: overlapp 2000-2039
        testFdato("010254 749 33", 1854) // 749: overlapp 2000-2039
        testFdato("010299 500 55", 1899) // 500: overlapp 2000-2039
        testFdato("040299 749 48", 1899) // 749: overlapp 2000-2039

        // 900-999, overlapp // 1940-1999
        testFdato("020240 900 46", 1940) // 900: overlapp 2000-2039
        testFdato("010240 999 34", 1940) // 999: overlapp 2000-2039
        testFdato("020299 900 45", 1999) // 900: overlapp 2000-2039
        testFdato("010299 999 33", 1999) // 999: overlapp 2000-2039
    }

    @Test
    fun `syntetiske fødselsnummer fra NAV og Skatt`() {
        testFdato("024200 000 69", 1900) // Nav/Dolly har +40 på måned
        testFdato("028200 000 69", 1900) // Skatt har +80 på måned
    }

    fun testFdato(
        fnr: String,
        year: Int,
    ) {
        assertThat(FoedselsNr(fnr.filter { it != ' ' }).foedselsdato.year).isEqualTo(year)
    }

    @Test
    fun foedseldatoDnummer() {
        val fnr = FoedselsNr("41021599938")
        val dato = LocalDate.of(2015, 2, 1)

        assertThat(fnr.foedselsdato).isEqualTo(dato)
    }
}
