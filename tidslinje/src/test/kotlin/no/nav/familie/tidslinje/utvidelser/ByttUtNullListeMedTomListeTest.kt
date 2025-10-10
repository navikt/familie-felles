package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ByttUtNullListeMedTomListeTest {
    private val femÅrSiden = LocalDate.now().minusYears(5)
    private val fireÅrOgÉnDagSiden = LocalDate.now().minusYears(4).minusDays(1)
    private val fireÅrSiden = LocalDate.now().minusYears(4)
    private val treÅrOgÉnDagSiden = LocalDate.now().minusYears(3).minusDays(1)
    private val treÅrSiden = LocalDate.now().minusYears(3)
    private val nå = LocalDate.now()

    @Test
    fun byttUtNullListeMedTomListe() {
        // Arrange
        val tidslinjeMedNullListe =
            listOf<Periode<List<String>?>>(
                Periode(listOf("a"), femÅrSiden, fireÅrOgÉnDagSiden),
                Periode(null, fireÅrSiden, treÅrOgÉnDagSiden),
                Periode(listOf("b"), treÅrSiden, nå),
            ).tilTidslinje()

        // Act
        val tidslinjeMedTomListeIstedetForNull = tidslinjeMedNullListe.tilPerioder().map { it.byttUtNullListeMedTomListe() }

        // Assert
        Assertions.assertNotNull(tidslinjeMedTomListeIstedetForNull.find { it.fom == fireÅrSiden }?.verdi)
        Assertions.assertEquals(emptyList<String>(), tidslinjeMedTomListeIstedetForNull.find { it.fom == fireÅrSiden }?.verdi)
    }
}
