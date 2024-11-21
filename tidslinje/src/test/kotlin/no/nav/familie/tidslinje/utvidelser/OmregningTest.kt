package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.TidsEnhet
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class OmregningTest {
    @Test
    fun `kan omgjøre fra uke til dag`() {
        val dato1Start = LocalDate.of(2022, 7, 4)
        val dato1Slutt = LocalDate.of(2022, 7, 10)

        val dato2Start = LocalDate.of(2022, 7, 11)
        val dato2Slutt = LocalDate.of(2022, 7, 17)

        val dato3Start = LocalDate.of(2022, 7, 18)
        val dato3Slutt = LocalDate.of(2022, 7, 24)

        val dato4Start = LocalDate.of(2022, 7, 25)
        val dato4Slutt = LocalDate.of(2022, 7, 31)

        val tmp =
            listOf(
                TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.WEEKS).toInt() + 1),
                TidslinjePeriode(2, dato2Start.until(dato2Slutt, ChronoUnit.WEEKS).toInt() + 1),
                TidslinjePeriode(3, dato3Start.until(dato3Slutt, ChronoUnit.WEEKS).toInt() + 1),
                TidslinjePeriode(4, dato4Start.until(dato4Slutt, ChronoUnit.WEEKS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp, tidsEnhet = TidsEnhet.UKE)

        val correct = listOf(1, 2, 3, 4)

        val tidslinjeDag = tidslinje.konverterTilDag()

        Assertions.assertEquals(correct, tidslinjeDag.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato1Start, tidslinjeDag.startsTidspunkt)

        Assertions.assertEquals(dato4Slutt, tidslinjeDag.kalkulerSluttTidspunkt())
    }

    @Test
    fun `kan omgjøre fra måned til dag`() {
        val dato1Start = LocalDate.of(2022, 7, 1)
        val dato1Slutt = LocalDate.of(2022, 7, 31)

        val dato2Start = LocalDate.of(2022, 8, 1)
        val dato2Slutt = LocalDate.of(2022, 8, 31)

        val dato3Start = LocalDate.of(2022, 9, 1)
        val dato3Slutt = LocalDate.of(2022, 9, 30)

        val dato4Start = LocalDate.of(2022, 10, 1)
        val dato4Slutt = LocalDate.of(2022, 10, 31)

        val tmp =
            listOf(
                TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.MONTHS).toInt() + 1),
                TidslinjePeriode(2, dato2Start.until(dato2Slutt, ChronoUnit.MONTHS).toInt() + 1),
                TidslinjePeriode(3, dato3Start.until(dato3Slutt, ChronoUnit.MONTHS).toInt() + 1),
                TidslinjePeriode(4, dato4Start.until(dato4Slutt, ChronoUnit.MONTHS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tmp, tidsEnhet = TidsEnhet.MÅNED)

        val correct = listOf(1, 2, 3, 4)

        val tidslinjeDag = tidslinje.konverterTilDag()

        Assertions.assertEquals(correct, tidslinjeDag.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato1Start, tidslinjeDag.startsTidspunkt)

        Assertions.assertEquals(dato4Slutt, tidslinjeDag.kalkulerSluttTidspunkt())
    }

    @Test
    fun `kan omgjøre fra år til dag`() {
        val dato1Start = LocalDate.of(2022, 1, 1)
        val dato1Slutt = LocalDate.of(2023, 12, 31)

        val dato2Start = LocalDate.of(2024, 1, 1)
        val dato2Slutt = LocalDate.of(2024, 12, 31)

        val dato3Start = LocalDate.of(2025, 1, 1)
        val dato3Slutt = LocalDate.of(2025, 12, 31)

        val dato4Start = LocalDate.of(2026, 1, 1)
        val dato4Slutt = LocalDate.of(2026, 12, 31)

        val tidslinjePerioder =
            listOf(
                TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.YEARS).toInt() + 1),
                TidslinjePeriode(2, dato2Start.until(dato2Slutt, ChronoUnit.YEARS).toInt() + 1),
                TidslinjePeriode(3, dato3Start.until(dato3Slutt, ChronoUnit.YEARS).toInt() + 1),
                TidslinjePeriode(4, dato4Start.until(dato4Slutt, ChronoUnit.YEARS).toInt() + 1),
            )

        val tidslinje = Tidslinje(dato1Start, tidslinjePerioder, tidsEnhet = TidsEnhet.ÅR)

        val forventedeVerdier = listOf(1, 2, 3, 4)

        val tidslinjeDag = tidslinje.konverterTilDag()

        Assertions.assertEquals(forventedeVerdier, tidslinjeDag.innhold.map { it.periodeVerdi.verdi }.toList())

        Assertions.assertEquals(dato1Start.withDayOfYear(1), tidslinjeDag.startsTidspunkt)

        Assertions.assertEquals(dato4Slutt, tidslinjeDag.kalkulerSluttTidspunkt())
    }

    @Test
    fun `Riktig start- og slutttidspunkt`() {
        val dato1Start = LocalDate.of(2022, 1, 1)
        val dato1Slutt = LocalDate.of(2023, 12, 31)

        val tmpÅR = listOf(TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.YEARS).toInt() + 1))
        val tmpMåned = listOf(TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.MONTHS).toInt() + 1))
        val tmpUke = listOf(TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.WEEKS).toInt() + 1))
        val tmpDag = listOf(TidslinjePeriode(1, dato1Start.until(dato1Slutt, ChronoUnit.DAYS).toInt() + 1))

        val tidslinjeÅR = Tidslinje(dato1Start, tmpÅR, tidsEnhet = TidsEnhet.ÅR)
        val tidslinjeMåned = Tidslinje(dato1Start, tmpMåned, tidsEnhet = TidsEnhet.MÅNED)
        val tidslinjeUke = Tidslinje(dato1Start, tmpUke, tidsEnhet = TidsEnhet.UKE)
        val tidslinjeDag = Tidslinje(dato1Start, tmpDag, tidsEnhet = TidsEnhet.DAG)

        Assertions.assertEquals(dato1Start, tidslinjeÅR.startsTidspunkt)
        Assertions.assertEquals(dato1Start, tidslinjeMåned.startsTidspunkt)
        Assertions.assertEquals(dato1Start, tidslinjeDag.startsTidspunkt)
        Assertions.assertEquals(LocalDate.of(2021, 12, 27), tidslinjeUke.startsTidspunkt)

        Assertions.assertEquals(dato1Slutt, tidslinjeÅR.kalkulerSluttTidspunkt())
        Assertions.assertEquals(dato1Slutt, tidslinjeMåned.kalkulerSluttTidspunkt())
        Assertions.assertEquals(dato1Slutt, tidslinjeUke.kalkulerSluttTidspunkt())
        Assertions.assertEquals(dato1Slutt, tidslinjeDag.kalkulerSluttTidspunkt())
    }
}
