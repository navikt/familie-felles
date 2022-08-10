package no.nav.familie

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FødselsnummerTest {

    @Test
    internal fun `skal tillate helsyntetiske nummer fra dolly`() {
        val listeAvBrukere = listOf(
            SyntetiskBruker("15507600333", "55507608360", "Mann", LocalDate.of(1976, 10, 15)),
            SyntetiskBruker("29422059278", "69422056629", "Kvinne", LocalDate.of(2020, 2, 29)),
            SyntetiskBruker("15507600333", "55507608360", "Mann", LocalDate.of(1976, 10, 15)),
            SyntetiskBruker("29422059278", "69422056629", "Kvinne", LocalDate.of(2020, 2, 29)),
            SyntetiskBruker("05440355678", "45440356293", "Kvinne", LocalDate.of(2003, 4, 5)),
            SyntetiskBruker("12429400544", "52429405181", "Mann", LocalDate.of(1994, 2, 12)),
            SyntetiskBruker("12505209719", "52505209540", "Mann", LocalDate.of(1952, 10, 12)),
            SyntetiskBruker("21483609245", "61483601467", "Kvinne", LocalDate.of(1936, 8, 21)),
            SyntetiskBruker("17912099997", "57912075186", "Mann", LocalDate.of(2020, 11, 17)),
            SyntetiskBruker("29822099635", "69822075096", "Kvinne", LocalDate.of(2020, 2, 29)),
            SyntetiskBruker("05840399895", "45840375084", "Kvinne", LocalDate.of(2003, 4, 5)),
            SyntetiskBruker("12829499914", "52829400197", "Mann", LocalDate.of(1994, 2, 12)),
            SyntetiskBruker("12905299938", "52905200100", "Mann", LocalDate.of(1952, 10, 12)),
            SyntetiskBruker("21883649874", "61883600222", "Kvinne", LocalDate.of(1936, 8, 21)),
        )

        listeAvBrukere.forEach {
            assertEquals(it.fnr, Fødselsnummer(it.fnr).verdi, "Fødselsnummer ${it.fnr} er gyldig")
            assertEquals(it.dnr, Fødselsnummer(it.dnr).verdi, "Dnr ${it.dnr} er gyldig")
            assertEquals(it.fødselsdato, Fødselsnummer(it.fnr).fødselsdato, "Finner dato for ${it.fnr}")
            assertEquals(it.fødselsdato, Fødselsnummer(it.dnr).fødselsdato, "Finner dato for ${it.dnr}")
            assertEquals(false, Fødselsnummer(it.fnr).erDNummer, "${it.fnr} er ikke D-nummer")
            assertEquals(true, Fødselsnummer(it.dnr).erDNummer, "${it.dnr} er D-nummer")
        }
    }

    data class SyntetiskBruker(val fnr: String, val dnr: String, val kjønn: String, val fødselsdato: LocalDate)
}
