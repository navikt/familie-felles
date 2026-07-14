package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.erIkkeTom
import no.nav.familie.tidslinje.harIkkeOverlappMed
import no.nav.familie.tidslinje.harOverlappMed
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KombinerUtenNullTest {
    private val jan = LocalDate.of(2022, 1, 1)
    private val des = LocalDate.of(2022, 12, 31)

    @Test
    fun `kombinerUtenNullMed kaller bare kombinator hvis begge tidslinjer har verdi`() {
        val venstre = listOf(Periode("a", jan, jan.plusMonths(5))).tilTidslinje()
        val høyre = listOf(Periode("b", jan.plusMonths(3), des)).tilTidslinje()

        val resultat = venstre.kombinerUtenNullMed(høyre) { v, h -> v + h }

        val perioder = resultat.tilPerioderIkkeNull()
        assertEquals(1, perioder.size)
        assertEquals("ab", perioder.single().verdi)
    }

    @Test
    fun `kombinerUtenNull filtrerer bort null-verdier før listeKombinator kalles`() {
        val a = listOf(Periode("a", jan, des)).tilTidslinje()
        val b = tomTidslinje<String>(startsTidspunkt = jan)

        val resultat = listOf(a, b).kombinerUtenNull { it.joinToString("") }

        assertEquals(listOf("a"), resultat.tilPerioder().map { it.verdi })
    }

    @Test
    fun `kombinerUtenNullOgIkkeTom kaller ikke listeKombinator hvis alle er null`() {
        val a = tomTidslinje<String>(startsTidspunkt = jan)
        val b = tomTidslinje<String>(startsTidspunkt = jan)

        val resultat = listOf(a, b).kombinerUtenNullOgIkkeTom { it.joinToString("") }

        assertTrue(resultat.erTom())
    }

    @Test
    fun `kombinerKunVerdiMed for map av tidslinjer kombinerer hver verdi med en felles tidslinje`() {
        val venstre = mapOf("a" to listOf(Periode(1, jan, des)).tilTidslinje())
        val høyre = listOf(Periode(10, jan, des)).tilTidslinje()

        val resultat = venstre.kombinerKunVerdiMed(høyre) { v, h -> v + h }

        assertEquals(listOf(11), resultat.getValue("a").tilPerioder().map { it.verdi })
    }

    @Test
    fun `kombinerKunVerdiMed for tre tidslinjer krever verdi i alle tre`() {
        val a = listOf(Periode(1, jan, des)).tilTidslinje()
        val b = listOf(Periode(2, jan, des)).tilTidslinje()
        val c = listOf(Periode(3, jan.plusMonths(6), des)).tilTidslinje()

        val resultat = a.kombinerKunVerdiMed(b, c) { x, y, z -> x + y + z }

        assertEquals(1, resultat.tilPerioderIkkeNull().size)
        assertEquals(6, resultat.tilPerioderIkkeNull().single().verdi)
    }

    @Test
    fun erIkkeTom() {
        assertFalse(tomTidslinje<Int>().erIkkeTom())
        assertTrue(listOf(Periode(1, jan, des)).tilTidslinje().erIkkeTom())
    }

    @Test
    fun `harOverlappMed og harIkkeOverlappMed`() {
        val a = listOf(Periode(1, jan, jan.plusMonths(5))).tilTidslinje()
        val overlappende = listOf(Periode(2, jan.plusMonths(3), des)).tilTidslinje()
        val ikkeOverlappende = listOf(Periode(2, des.plusDays(1), des.plusMonths(2))).tilTidslinje()

        assertTrue(a.harOverlappMed(overlappende))
        assertFalse(a.harIkkeOverlappMed(overlappende))

        assertFalse(a.harOverlappMed(ikkeOverlappende))
        assertTrue(a.harIkkeOverlappMed(ikkeOverlappende))
    }

    @Test
    fun `kombinerMedNullable returnerer this uendret hvis den andre tidslinjen er null`() {
        val a = listOf(Periode(1, jan, des)).tilTidslinje()
        val nullTidslinje: Tidslinje<Int>? = null

        val resultat = a.kombinerMedNullable(nullTidslinje) { v, _ -> v }

        assertEquals(a, resultat)
    }

    @Test
    fun `kombinerMedNullable kombinerer normalt hvis den andre tidslinjen ikke er null`() {
        val a = listOf(Periode(1, jan, des)).tilTidslinje()
        val b = listOf(Periode(2, jan, des)).tilTidslinje()

        val resultat = a.kombinerMedNullable(b) { v1, v2 -> (v1 ?: 0) + (v2 ?: 0) }

        assertEquals(listOf(3), resultat.tilPerioder().map { it.verdi })
    }
}
