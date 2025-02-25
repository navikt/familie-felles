package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TidslinjeListeTest {
    private var lst1 = emptyList<TidslinjePeriode<Boolean?>>()
    private var lst2 = emptyList<TidslinjePeriode<Boolean?>>()
    private var lst3 = emptyList<TidslinjePeriode<Boolean?>>()
    private val nullVerdi = null

    private var tidslinjeList: List<Tidslinje<Boolean?>> =
        listOf(Tidslinje(LocalDate.now(), emptyList()), Tidslinje(LocalDate.now(), emptyList()))

    private fun init(
        lst1: List<TidslinjePeriode<Boolean?>>,
        lst2: List<TidslinjePeriode<Boolean?>>,
        lst3: List<TidslinjePeriode<Boolean?>>,
    ) {
        this.lst1 = lst1
        this.lst2 = lst2
        this.lst3 = lst3
        this.tidslinjeList =
            listOf(Tidslinje(LocalDate.now(), lst1), Tidslinje(LocalDate.now(), lst2), Tidslinje(LocalDate.now(), lst3))
    }

    private fun initUlikStartSlutt(
        lst1: List<TidslinjePeriode<Boolean?>>,
        lst2: List<TidslinjePeriode<Boolean?>>,
        lst3: List<TidslinjePeriode<Boolean?>>,
    ) {
        this.lst1 = lst1
        this.lst2 = lst2
        this.lst3 = lst3
        this.tidslinjeList =
            listOf(
                Tidslinje(LocalDate.now().minusDays(2), lst1),
                Tidslinje(LocalDate.now().plusDays(1), lst2),
                Tidslinje(LocalDate.now(), lst3),
            )
    }

    @Test
    fun `kan slå sammen flere tidslinjer av samme type`() {
        init(
            listOf(TidslinjePeriode(true, 3, false), TidslinjePeriode(false, 1, false)),
            listOf(TidslinjePeriode(true, 1, false), TidslinjePeriode(true, 1, false)),
            listOf(TidslinjePeriode(false, 1, false), TidslinjePeriode(true, 1, false)),
        )
        val test =
            tidslinjeList.slåSammenLikeTidslinjer { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! && el2.verdi!!)
                }
            }

        val fakta = mutableListOf(false, true, nullVerdi)

        Assertions.assertEquals(
            fakta,
            test.innhold.map { it.periodeVerdi.verdi }.toList(),
            "klarte ikke å slå sammen flere lister",
        )
    }

    @Test
    fun `enkel test for å sjekke at true og false TidslinjePerioder ikke slås sammen til en`() {
        val tidslinjeTest: Tidslinje<Boolean?> =
            Tidslinje(LocalDate.now(), listOf(TidslinjePeriode(true, 1, false), TidslinjePeriode(false, 1, false)))

        val fakta = mutableListOf(true, false)

        Assertions.assertEquals(
            fakta,
            tidslinjeTest.innhold.map { it.periodeVerdi.verdi }.toList(),
            "klarte ikke håndtere lister med true og false",
        )
    }

    @Test
    fun `kan slå sammen flere tidslinjer av samme type med ulik start og slutt`() {
        initUlikStartSlutt(
            listOf(TidslinjePeriode(true, 3, false), TidslinjePeriode(false, 1, false)),
            listOf(TidslinjePeriode(true, 2, false), TidslinjePeriode(true, 1, false)),
            listOf(TidslinjePeriode(false, 2, false), TidslinjePeriode(true, 1, false)),
        )
        val test =
            tidslinjeList.slåSammenLikeTidslinjer { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! && el2.verdi!!)
                }
            }

        val fakta = mutableListOf(nullVerdi, false, nullVerdi)

        Assertions.assertEquals(
            fakta,
            test.innhold.map { it.periodeVerdi.verdi }.toList(),
            "klarte ikke slå sammen tre lister av samme type med ulik start og slutt",
        )
    }

    @Test
    fun `sjekke at startdato settes riktig av slåSammenLikeTidslinjer`() {
        var start1 = LocalDate.of(2022, 6, 1)
        var stopp1 = LocalDate.of(2022, 7, 31)

        var start2 = LocalDate.of(2022, 8, 1)
        var stopp2 = LocalDate.of(2022, 9, 30)

        val t1 =
            Tidslinje(
                start1,
                listOf(
                    TidslinjePeriode(true, start1.until(stopp1, ChronoUnit.DAYS) + 1),
                    TidslinjePeriode(false, start2.until(stopp2, ChronoUnit.DAYS) + 1),
                ),
            )

        start1 = LocalDate.of(2022, 5, 1)
        stopp1 = LocalDate.of(2022, 6, 30)

        start2 = LocalDate.of(2022, 7, 1)
        stopp2 = LocalDate.of(2022, 8, 31)

        val t2 =
            Tidslinje(
                start1,
                listOf(
                    TidslinjePeriode(true, start1.until(stopp1, ChronoUnit.DAYS) + 1),
                    TidslinjePeriode(false, start2.until(stopp2, ChronoUnit.DAYS) + 1),
                ),
            )

        val tidslinjeList: List<Tidslinje<Boolean>> = listOf(t1, t2)
        val result =
            tidslinjeList.slåSammenLikeTidslinjer { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! && el2.verdi!!)
                }
            }
        Assertions.assertEquals(LocalDate.of(2022, 5, 1), result.startsTidspunkt)
    }
}
