package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.INF
import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjeNullUndefTest {
    private var lst1 = emptyList<TidslinjePeriode<Int>>()
    private var lst2 = emptyList<TidslinjePeriode<Int>>()

    private var t1: Tidslinje<Int> = Tidslinje(LocalDate.now(), emptyList())
    private var t2: Tidslinje<Int> = Tidslinje(LocalDate.now(), emptyList())

    private fun init(
        lst1: List<TidslinjePeriode<Int>>,
        lst2: List<TidslinjePeriode<Int>>,
    ) {
        this.lst1 = lst1
        this.lst2 = lst2
        this.t1 = Tidslinje(LocalDate.now(), lst1)
        this.t2 = Tidslinje(LocalDate.now(), lst2)
    }

    @Test
    fun `kan legge sammen tidslinjer av ulik lengde hvor den ene da blir paddet med udefinert`() {
        init(
            listOf(
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(null, 1, false),
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(2, 2, false),
            ),
            listOf(TidslinjePeriode(3, 1, false), TidslinjePeriode(1, 1, false), TidslinjePeriode(1, 1, false)),
        )

        val testTidslinje =
            t1.biFunksjon(t2) { t1, t2 ->
                if (t1 is Udefinert || t2 is Udefinert) {
                    Udefinert()
                } else if (t1 is Null || t2 is Null) {
                    Null()
                } else {
                    Verdi(t1.verdi!! + t2.verdi!!)
                }
            }

        val fakta = mutableListOf(4, null, 2, null)

        Assertions.assertEquals(
            fakta,
            testTidslinje.innhold.map { it.periodeVerdi.verdi }.toList(),
            "klarte ikke legge sammen listene",
        )
        Assertions.assertInstanceOf(Udefinert::class.java, testTidslinje.innhold.last().periodeVerdi)
        Assertions.assertInstanceOf(Null::class.java, testTidslinje.innhold[1].periodeVerdi)
    }

    @Test
    fun `kan legge sammen tidslinjer av samme lengde med null-verdier`() {
        init(
            listOf(TidslinjePeriode(Udefinert(), 3, false), TidslinjePeriode(2, 1, false), TidslinjePeriode(99, INF, true)),
            listOf(TidslinjePeriode(3, 2, false), TidslinjePeriode(1, 1, false), TidslinjePeriode(1, 1, false)),
        )

        val testTidslinje =
            t1.biFunksjon(t2) { t1, t2 ->
                if (t1 is Null || t2 is Null) {
                    Null()
                } else if (t1 is Udefinert || t2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(t1.verdi!! + t2.verdi!!)
                }
            }

        val fakta = mutableListOf(null, 3, null)

        Assertions.assertEquals(
            fakta,
            testTidslinje.innhold.map { it.periodeVerdi.verdi }.toList(),
            "K",
        )
        Assertions.assertInstanceOf(Udefinert::class.java, testTidslinje.innhold.last().periodeVerdi)
        Assertions.assertInstanceOf(Udefinert::class.java, testTidslinje.innhold.first().periodeVerdi)
        assertTrue { testTidslinje.innhold.last().erUendelig }
    }
}
