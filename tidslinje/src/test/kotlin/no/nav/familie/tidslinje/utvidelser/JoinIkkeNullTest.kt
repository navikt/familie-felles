package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JoinIkkeNullTest {
    private val jan = LocalDate.of(2022, 1, 1)
    private val des = LocalDate.of(2022, 12, 31)

    @Test
    fun `joinIkkeNull kombinerer to map'er per nøkkel og dropper nøkler som bare finnes i den ene`() {
        val venstre = mapOf("a" to listOf(Periode(1, jan, des)).tilTidslinje(), "b" to listOf(Periode(2, jan, des)).tilTidslinje())
        val høyre = mapOf("a" to listOf(Periode(10, jan, des)).tilTidslinje(), "c" to listOf(Periode(20, jan, des)).tilTidslinje())

        val resultat = venstre.joinIkkeNull(høyre) { v, h -> v + h }

        assertEquals(setOf("a"), resultat.keys)
        assertEquals(listOf(Periode(11, jan, des)), resultat.getValue("a").tilPerioder())
    }

    @Test
    fun `joinIkkeNull kaller ikke kombinator hvis en av sidene mangler verdi for et tidspunkt`() {
        val venstre = mapOf("a" to listOf(Periode(1, jan, jan.plusMonths(5))).tilTidslinje())
        val høyre = mapOf("a" to listOf(Periode(10, jan.plusMonths(3), des)).tilTidslinje())

        val resultat = venstre.joinIkkeNull(høyre) { v, h -> v + h }

        val perioder = resultat.getValue("a").tilPerioderIkkeNull()
        assertEquals(1, perioder.size)
        assertEquals(11, perioder.single().verdi)
    }
}
