package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FiltrerTidslinjerUtvidetTest {
    private val jan = LocalDate.of(2022, 1, 1)
    private val des = LocalDate.of(2022, 12, 31)

    @Test
    fun `filtrerIkkeNull med predikat filtrerer vekk verdier som ikke oppfyller filteret`() {
        val tidslinje =
            listOf(
                Periode(1, jan, jan.plusMonths(5)),
                Periode(2, jan.plusMonths(6), des),
            ).tilTidslinje()

        val resultat = tidslinje.filtrerIkkeNull { it > 1 }

        assertEquals(listOf(2), resultat.tilPerioderIkkeNull().map { it.verdi })
    }

    @Test
    fun `filtrerMed setter verdi til null der den boolske tidslinjen er false`() {
        val verdier = listOf(Periode("a", jan, des)).tilTidslinje()
        val boolsk = listOf(Periode(true, jan, jan.plusMonths(5)), Periode(false, jan.plusMonths(6), des)).tilTidslinje()

        val resultat = verdier.filtrerMed(boolsk)

        assertEquals(listOf("a"), resultat.tilPerioderIkkeNull().map { it.verdi })
    }

    @Test
    fun `filtrerHverKunVerdi filtrerer alle tidslinjer i et map`() {
        val map =
            mapOf(
                "a" to listOf(Periode(1, jan, des)).tilTidslinje(),
                "b" to listOf(Periode(5, jan, des)).tilTidslinje(),
            )

        val resultat = map.filtrerHverKunVerdi { it > 2 }

        assertEquals(0, resultat.getValue("a").tilPerioderIkkeNull().size)
        assertEquals(1, resultat.getValue("b").tilPerioderIkkeNull().size)
    }
}
