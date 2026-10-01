package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.INF
import no.nav.familie.tidslinje.PRAKTISK_SENESTE_DAG
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeskjærTidslinjerTest {
    private val jan = LocalDate.of(2022, 1, 1)
    private val jun = LocalDate.of(2022, 6, 30)
    private val des = LocalDate.of(2022, 12, 31)

    @Test
    fun `beskjærTilOgMedEtter forkorter tidslinjen til sluttidspunktet til den andre tidslinjen`() {
        val tidslinje = listOf(Periode("a", jan, des)).tilTidslinje()
        val annen = listOf(Periode("b", jan, jun)).tilTidslinje()

        val resultat = tidslinje.beskjærTilOgMedEtter(annen)

        assertEquals(jun, resultat.kalkulerSluttTidspunkt())
    }

    @Test
    fun `beskjærTilOgMedEtter mot tom tidslinje gir tom tidslinje`() {
        val tidslinje = listOf(Periode("a", jan, des)).tilTidslinje()

        val resultat = tidslinje.beskjærTilOgMedEtter(tomTidslinje<String>())

        assertTrue(resultat.erTom())
    }

    @Test
    fun `beskjær forkorter til gitt fom og tom`() {
        val tidslinje = listOf(Periode("a", jan, des)).tilTidslinje()

        val resultat = tidslinje.beskjær(jan.plusMonths(1), jun)

        assertEquals(jan.plusMonths(1), resultat.startsTidspunkt)
        assertEquals(jun, resultat.kalkulerSluttTidspunkt())
    }

    @Test
    fun `beskjærFraOgMed forkorter bare start`() {
        val tidslinje = listOf(Periode("a", jan, des)).tilTidslinje()

        val resultat = tidslinje.beskjærFraOgMed(jun)

        assertEquals(jun, resultat.startsTidspunkt)
        assertEquals(des, resultat.kalkulerSluttTidspunkt())
    }

    @Test
    fun `beskjærTilOgMed forkorter bare slutt`() {
        val tidslinje = listOf(Periode("a", jan, des)).tilTidslinje()

        val resultat = tidslinje.beskjærTilOgMed(jun)

        assertEquals(jan, resultat.startsTidspunkt)
        assertEquals(jun, resultat.kalkulerSluttTidspunkt())
    }

    @Test
    fun `beskjærTilOgMed for map av tidslinjer beskjærer alle verdiene`() {
        val map =
            mapOf(
                "a" to listOf(Periode(1, jan, des)).tilTidslinje(),
                "b" to listOf(Periode(2, jan, des)).tilTidslinje(),
            )

        val resultat = map.beskjærTilOgMed(jun)

        assertEquals(jun, resultat.getValue("a").kalkulerSluttTidspunkt())
        assertEquals(jun, resultat.getValue("b").kalkulerSluttTidspunkt())
    }

    @Test
    fun `forlengFremtidTilUendelig forlenger siste periode til uendelig hvis den strekker seg forbi tidspunktet`() {
        val tidslinje =
            listOf(
                Periode("a", jan, jun),
                Periode("b", jun.plusDays(1), des),
            ).tilTidslinje()

        val resultat = tidslinje.forlengFremtidTilUendelig(des.minusMonths(1))

        val perioder = resultat.tilPerioderIkkeNull()
        assertEquals(2, perioder.size)
        assertEquals(null, perioder.last().tom)
    }

    @Test
    fun `forlengFremtidTilUendelig gjør ingenting hvis siste periode ikke strekker seg forbi tidspunktet`() {
        val tidslinje = listOf(Periode("a", jan, jun)).tilTidslinje()

        val resultat = tidslinje.forlengFremtidTilUendelig(des)

        assertEquals(jun, resultat.tilPerioderIkkeNull().single().tom)
    }

    @Test
    fun `kalkulerSluttTidspunkt gir praktisk seneste dag for uendelig tidslinje uansett hvordan den er laget`() {
        val fraPeriode = Periode("a", LocalDate.of(2020, 1, 1), null).tilTidslinje()
        val fraLengde = Tidslinje(LocalDate.of(2019, 1, 1), listOf(TidslinjePeriode("b", INF, true)))

        assertEquals(PRAKTISK_SENESTE_DAG, fraPeriode.kalkulerSluttTidspunkt())
        assertEquals(PRAKTISK_SENESTE_DAG, fraLengde.kalkulerSluttTidspunkt())
    }

    @Test
    fun `beskjærTilOgMedEtter mot uendelig tidslinje beholder uendelighet`() {
        val tidslinje = Periode("a", LocalDate.of(2020, 1, 1), null).tilTidslinje()
        val uendelig = Tidslinje(LocalDate.of(2019, 1, 1), listOf(TidslinjePeriode("b", INF, true)))

        val resultat = tidslinje.beskjærTilOgMedEtter(uendelig).tilPerioder()

        assertEquals(listOf(Periode("a", LocalDate.of(2020, 1, 1), null)), resultat)
    }
}
