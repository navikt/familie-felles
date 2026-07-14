package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EndreTidTest {
    @Test
    fun `tilMåned konverterer en dag-basert tidslinje til en måned-basert tidslinje`() {
        val dagTidslinje =
            listOf(
                Periode(1, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 31)),
                Periode(2, LocalDate.of(2022, 2, 1), LocalDate.of(2022, 2, 28)),
            ).tilTidslinje()

        val månedTidslinje = dagTidslinje.tilMåned { it.filterNotNull().sum() }

        assertEquals(listOf(1, 2), månedTidslinje.tilPerioder().map { it.verdi })
    }

    @Test
    fun `tilMånedFraMånedsskifteIkkeNull gir tom tidslinje hvis alle dager er innenfor én måned`() {
        val dagTidslinje = listOf(Periode("a", LocalDate.of(2021, 12, 7), LocalDate.of(2021, 12, 12))).tilTidslinje()

        val månedTidslinje = dagTidslinje.tilMånedFraMånedsskifteIkkeNull { _, _ -> "b" }

        assertTrue(månedTidslinje.erTom())
    }

    @Test
    fun `tilMånedFraMånedsskifteIkkeNull gir en måned ved ett månedsskifte`() {
        val dagTidslinje = listOf(Periode("x", LocalDate.of(2021, 11, 28), LocalDate.of(2021, 12, 4))).tilTidslinje()

        val månedTidslinje = dagTidslinje.tilMånedFraMånedsskifteIkkeNull { _, verdiFørsteDagDenneMåned -> verdiFørsteDagDenneMåned }

        val perioder = månedTidslinje.tilPerioderIkkeNull()
        assertEquals(1, perioder.size)
        assertEquals("x", perioder.single().verdi)
    }

    @Test
    fun `tilMånedFraMånedsskifte håndterer at én av dagene mangler verdi`() {
        val dagTidslinje = tomTidslinje<String>(startsTidspunkt = LocalDate.of(2022, 1, 1))

        val månedTidslinje = dagTidslinje.tilMånedFraMånedsskifte { _, _ -> "uansett" }

        assertTrue(månedTidslinje.erTom())
    }

    @Test
    fun `forlengTidslinjeMedEnMåned legger til en ekstra tom måned etter tidslinjens slutt`() {
        val tidslinje = listOf(Periode("a", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 31))).tilTidslinje()

        val resultat = tidslinje.forlengTidslinjeMedEnMåned()

        assertEquals(LocalDate.of(2022, 3, 1), resultat.kalkulerSluttTidspunkt())
    }

    @Test
    fun `forlengTidslinjeMedEnMåned gjør ingenting hvis tidslinjen allerede er uendelig`() {
        val tidslinje = Periode("a", LocalDate.of(2022, 1, 1), null).tilTidslinje()

        val resultat = tidslinje.forlengTidslinjeMedEnMåned()

        assertEquals(tidslinje, resultat)
    }
}
