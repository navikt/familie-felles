package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.INF
import no.nav.familie.tidslinje.TidsEnhet
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class KonverterTidTest {
    @Test
    fun `Kan gå fra dager til måneder`() {
        val dato1Start = LocalDate.of(2022, 7, 1)
        val dato1Slutt = LocalDate.of(2022, 7, 31)

        val dato2Start = LocalDate.of(2022, 8, 1)
        val dato2Slutt = LocalDate.of(2022, 8, 31)

        val tmp =
            listOf(
                TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(2, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned = tidslinje.konverterTilMåned { _, it -> it.last().last().periodeVerdi }.høyreShift()
        val correct = listOf(1, 2)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato2Start, tidslinjeMåned.startsTidspunkt)
    }

    @Test
    fun `Kan gå fra dager til måneder test 2`() {
        val dato1Start = LocalDate.of(2022, 7, 5)
        val dato1Slutt = LocalDate.of(2022, 8, 4)

        val dato2Start = LocalDate.of(2022, 8, 5)
        val dato2Slutt = LocalDate.of(2022, 9, 3)

        val dato3Start = LocalDate.of(2022, 9, 4)
        val dato3Slutt = LocalDate.of(2022, 10, 31)

        val dato4Start = LocalDate.of(2022, 11, 1)
        val dato4Slutt = LocalDate.of(2022, 11, 30)

        val tmp =
            listOf(
                TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(2, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(3, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(4, dato4Start.until(dato4Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned = tidslinje.konverterTilMåned { _, it -> it.last().last().periodeVerdi }.høyreShift()
        val correct = listOf(1, 2, 3, 4)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato2Start.withDayOfMonth(1), tidslinjeMåned.startsTidspunkt)
    }

    @Test
    fun `kan konvertere til måneder når det er flere TidslinjePerioder i løpet av en måned`() {
        val dato1Start = LocalDate.of(2022, 7, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 20)

        val dato3Start = LocalDate.of(2022, 7, 21)
        val dato3Slutt = LocalDate.of(2022, 10, 31)

        val dato4Start = LocalDate.of(2022, 11, 1)
        val dato4Slutt = LocalDate.of(2022, 11, 30)

        val tmp =
            listOf(
                TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(2, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(3, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(4, dato4Start.until(dato4Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned = tidslinje.konverterTilMåned { _, it -> it.last().last().periodeVerdi }.høyreShift()
        val correct = listOf(3, 4)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato1Start.plusMonths(1).withDayOfMonth(1), tidslinjeMåned.startsTidspunkt)
    }

    @Test
    fun `flere måneder med flere TidslinjePerioder i hver måned`() {
        val dato1Start = LocalDate.of(2022, 7, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 8, 11)

        val dato4Start = LocalDate.of(2022, 8, 12)
        val dato4Slutt = LocalDate.of(2022, 8, 31)

        val dato5Start = LocalDate.of(2022, 9, 1)
        val dato5Slutt = LocalDate.of(2022, 9, 3)

        val tmp =
            listOf(
                TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(2, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(3, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(4, dato4Start.until(dato4Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(5, dato5Start.until(dato5Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned = tidslinje.konverterTilMåned { _, it -> it.last().last().periodeVerdi }.høyreShift()
        val correct = listOf(2, 4, 5)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato1Start.plusMonths(1).withDayOfMonth(1), tidslinjeMåned.startsTidspunkt)
    }

    @Test
    fun `TidslinjePeriode går over flere måneder på slutten`() {
        val dato1Start = LocalDate.of(2022, 7, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val dato4Start = LocalDate.of(2022, 10, 12)
        val dato4Slutt = LocalDate.of(2022, 11, 30)

        val dato5Start = LocalDate.of(2022, 12, 1)
        val dato5Slutt = LocalDate.of(2023, 3, 5)

        val tmp =
            listOf(
                TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(2, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(3, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(4, dato4Start.until(dato4Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(5, dato5Start.until(dato5Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned = tidslinje.splittPåMåned()
        val correct = listOf(2, 3, 3, 4, 4, 5, 5, 5, 5)

        Assertions.assertEquals(correct, tidslinjeMåned.map { it.last().periodeVerdi.verdi }.toList())
    }

    @Test
    fun `TidslinjePeriode går over flere måneder på slutten gg`() {
        val dato1Start = LocalDate.of(2022, 7, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val dato4Start = LocalDate.of(2022, 10, 12)
        val dato4Slutt = LocalDate.of(2022, 11, 30)

        val dato5Start = LocalDate.of(2022, 12, 1)
        val dato5Slutt = LocalDate.of(2023, 3, 5)

        val tmp =
            listOf(
                TidslinjePeriode(1.0, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(2.0, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(3.0, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(4.0, dato4Start.until(dato4Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(5.0, dato5Start.until(dato5Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned =
            tidslinje.konverterTilMåned { _, it ->
                Verdi(
                    it
                        .last()
                        .map { it.periodeVerdi.verdi!! }
                        .toList()
                        .average(),
                )
            }
        val correct: List<Double> = listOf(1.5, 3.0, 3.5, 4.0, 5.0)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())
    }

    @Test
    fun `kunne beregne månedlig valutakurs fra dagskurser`() {
        val test =
            Tidslinje.lagTidslinjeFraListe(
                listOf(
                    1.0,
                    2.0,
                    3.0,
                    4.0,
                    5.0,
                    6.0,
                    7.0,
                    8.0,
                    9.0,
                    10.0,
                    11.0,
                    12.0,
                    13.0,
                    14.0,
                    15.0,
                    16.0,
                    17.0,
                    18.0,
                    19.0,
                    20.0,
                    21.0,
                    22.0,
                    23.0,
                    24.0,
                    25.0,
                    26.0,
                    27.0,
                    28.0,
                    29.0,
                    30.0,
                    31.0,
                    1.0,
                    2.0,
                    3.0,
                    4.0,
                    5.0,
                    6.0,
                    7.0,
                    8.0,
                    9.0,
                    10.0,
                    11.0,
                    12.0,
                    13.0,
                    14.0,
                    15.0,
                    16.0,
                    17.0,
                    18.0,
                    19.0,
                    20.0,
                    21.0,
                    22.0,
                    23.0,
                    24.0,
                    25.0,
                    26.0,
                    27.0,
                    28.0,
                    1.0,
                    2.0,
                    3.0,
                    4.0,
                    5.0,
                    6.0,
                    7.0,
                    8.0,
                    9.0,
                    10.0,
                    11.0,
                    12.0,
                    13.0,
                    14.0,
                    15.0,
                    16.0,
                    17.0,
                    18.0,
                    19.0,
                    20.0,
                    21.0,
                    22.0,
                    23.0,
                    24.0,
                    25.0,
                    26.0,
                    27.0,
                    28.0,
                    29.0,
                    30.0,
                    31.0,
                ),
                LocalDate.of(2022, 1, 1),
                TidsEnhet.DAG,
            )

        val tidslinjeMåned =
            test
                .konverterTilMåned { _, it ->
                    Verdi(
                        it
                            .last()
                            .map { it.periodeVerdi.verdi!! }
                            .toList()
                            .average(),
                    )
                }.høyreShift()
        val correct = listOf(16.0, 14.5, 16.0)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(LocalDate.of(2022, 1, 1).plusMonths(1).withDayOfMonth(1), tidslinjeMåned.startsTidspunkt)
    }

    @Test
    fun `tester med  operatoren verdien som har vart lengst blir valgt for inneværende TidslinjePeriode`() {
        val dato1Start = LocalDate.of(2022, 6, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val dato4Start = LocalDate.of(2022, 10, 12)
        val dato4Slutt = LocalDate.of(2022, 11, 30)

        val dato5Start = LocalDate.of(2022, 12, 1)
        val dato5Slutt = LocalDate.of(2023, 3, 5)

        val tmp =
            listOf(
                TidslinjePeriode(1.0, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(2.0, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(3.0, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(4.0, dato4Start.until(dato4Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(5.0, dato5Start.until(dato5Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val splittPåMåned = tidslinje.splittPåMåned()

        val tidslinjeMåned = tidslinje.konverterTilMåned { _, it -> it.last().maxBy { it.lengde }.periodeVerdi }

        val correctBeforeTidslinje: List<Double> =
            listOf(
                1.0,
                2.0,
                3.0,
                3.0,
                4.0,
                4.0,
                5.0,
                5.0,
                5.0,
                5.0,
            ) // Dette er sånn lista skulle vært, men siden Tidslinje slår sammen like blir det
        val correct: List<Double> = listOf(1.0, 2.0, 3.0, 4.0, 5.0)

        // Assertions.assertEquals(tidslinjeMåned.innhold[2].lengde, (dato3Start.until(LocalDate.of(2022, 9, 30), ChronoUnit.DAYS).toInt() + 1))
        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())
        Assertions.assertEquals(correctBeforeTidslinje, splittPåMåned.map { it.maxBy { periode -> periode.lengde }.periodeVerdi.verdi })
    }

    @Test
    fun `tester med  boolske verdier`() {
        val dato1Start = LocalDate.of(2022, 6, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val dato4Start = LocalDate.of(2022, 10, 12)
        val dato4Slutt = LocalDate.of(2022, 11, 30)

        val dato5Start = LocalDate.of(2022, 12, 1)
        val dato5Slutt = LocalDate.of(2023, 3, 5)

        val tmp =
            listOf(
                TidslinjePeriode(true, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(false, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(true, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(true, dato4Start.until(dato4Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(false, dato5Start.until(dato5Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned = tidslinje.konverterTilMåned { _, it -> it.last().last().periodeVerdi }

        val correct: List<Boolean> =
            listOf(true, false, true, false) // Dette er sånn lista skulle vært, men siden Tidslinje slår sammen like blir det

        Assertions.assertEquals(tidslinjeMåned.innhold[2].lengde, 4)
        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())
        // Assertions.assertEquals(correctBeforeTidslinje, splittPåMåned.map { it.maxBy { it.lengde }.periodeVerdi.verdi })
    }

    @Test
    fun `Kan håndtere tidslinjer med uendelig slutt`() {
        val dato1Start = LocalDate.of(2022, 6, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val dato4Start = LocalDate.of(2022, 10, 12)
        val dato4Slutt = LocalDate.of(2022, 11, 10)

        val dato5Start = LocalDate.of(2022, 11, 11)
        val dato5Slutt = LocalDate.of(2023, 3, 5)

        val tmp =
            listOf(
                TidslinjePeriode(1.0, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(2.0, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(3.0, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(4.0, dato4Start.until(dato4Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(5.0, dato5Start.until(dato5Slutt, ChronoUnit.DAYS).toInt() + 1, true),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val splittPåMåned = tidslinje.splittPåMåned()

        val tidslinjeMåned =
            tidslinje.konverterTilMåned { _, it ->
                it.last().maxBy { it.lengde }.periodeVerdi
            } // velger den verdien som varer lengst innad i en måned.

        val correctBeforeTidslinje: List<Double> =
            listOf(
                1.0,
                2.0,
                3.0,
                3.0,
                4.0,
                5.0,
                5.0,
            ) // Dette er sånn lista skulle vært, men siden Tidslinje slår sammen like blir det
        val correct: List<Double> = listOf(1.0, 2.0, 3.0, 4.0, 5.0)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi }.toList())
        Assertions.assertEquals(1, tidslinjeMåned.innhold[3].lengde)
        assertTrue { INF <= tidslinjeMåned.innhold[4].lengde }
        Assertions.assertEquals(correctBeforeTidslinje, splittPåMåned.map { it.maxBy { periode -> periode.lengde }.periodeVerdi.verdi })
        assertTrue(tidslinjeMåned.innhold.last().erUendelig)
    }

    @Test
    fun `kan ta gjennomsnittet over to måneder for å bestemme verdien til en måned`() {
        val dato1Start = LocalDate.of(2022, 6, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val tmp =
            listOf(
                TidslinjePeriode(5.0, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(7.0, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(15.0, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned =
            tidslinje.konverterTilMåned(1) { _, vindu ->
                if (vindu[0].isEmpty()) {
                    Verdi(vindu[1].last().periodeVerdi.verdi!!)
                } else {
                    val avg = (vindu[0].last().periodeVerdi.verdi!! + vindu[1].last().periodeVerdi.verdi!!) / 2
                    Verdi(avg)
                }
            }

        val correct: List<Int> = listOf(5, (5 + 7) / 2, (7 + 15) / 2, 15)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi!!.toInt() }.toList())
    }

    @Test
    fun `kan spesifisere at januar skal få en annen verdi enn andre måneder`() {
        val dato1Start = LocalDate.of(2022, 6, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val tmp =
            listOf(
                TidslinjePeriode(5.0, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(7.0, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(15.0, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        var tidslinjeMåned =
            tidslinje.konverterTilMåned(2) { dato, _ -> if (dato.month == java.time.Month.JULY) Verdi(1.0) else Verdi(0.0) }

        var correct = listOf(0.0, 1.0, 0.0)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi!! }.toList())

        tidslinjeMåned =
            tidslinje.konverterTilMåned(2) { dato, _ -> if (dato.month == java.time.Month.OCTOBER) Verdi(1.0) else Verdi(0.0) }

        correct = listOf(0.0, 1.0)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi!! }.toList())
    }

    @Test
    fun `kan kovertere til måned med window 1 på begge sider når siste mnd er uendelig`() {
        val dato1Start = LocalDate.of(2022, 6, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val tmp =
            listOf(
                TidslinjePeriode(5.0, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(7.0, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(15.0, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1, erUendelig = true),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned =
            tidslinje.konverterTilMåned(antallMndBakoverITid = 1, antallMndFremoverITid = 1) { _, vindu ->
                if (vindu[0].isEmpty()) {
                    Verdi((vindu[1].last().periodeVerdi.verdi!! + vindu[2].last().periodeVerdi.verdi!!) / 2)
                } else if (vindu[2].isEmpty()) {
                    Verdi((vindu[0].last().periodeVerdi.verdi!! + vindu[1].last().periodeVerdi.verdi!!) / 2)
                } else {
                    val avg =
                        (vindu[0].last().periodeVerdi.verdi!! + vindu[1].last().periodeVerdi.verdi!! + vindu[2].last().periodeVerdi.verdi!!) / 3
                    Verdi(avg)
                }
            }

        val correct: List<Int> = listOf((5 + 7) / 2, (5 + 7 + 15) / 3, (7 + 15) / 2, 15)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi!!.toInt() }.toList())
    }

    @Test
    fun `kan kovertere til måned med window kun ett element og uendelig ende`() {
        val dato1Start = LocalDate.of(2022, 6, 5)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 31)

        val dato3Start = LocalDate.of(2022, 8, 1)
        val dato3Slutt = LocalDate.of(2022, 10, 11)

        val tmp =
            listOf(
                TidslinjePeriode(5.0, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(7.0, dato2Start.until(dato2Slutt, ChronoUnit.DAYS).toInt() + 1),
                TidslinjePeriode(15.0, dato3Start.until(dato3Slutt, ChronoUnit.DAYS).toInt() + 1, erUendelig = true),
            )

        val tidslinje = Tidslinje(dato1Start, tmp)

        val tidslinjeMåned =
            tidslinje.konverterTilMåned { _, vindu ->
                Verdi(vindu[0].last().periodeVerdi.verdi!!)
            }

        val correct: List<Int> = listOf(5, 7, 15)

        Assertions.assertEquals(correct, tidslinjeMåned.innhold.map { it.periodeVerdi.verdi!!.toInt() }.toList())
        assertTrue(tidslinjeMåned.innhold.last().erUendelig)
    }
}
