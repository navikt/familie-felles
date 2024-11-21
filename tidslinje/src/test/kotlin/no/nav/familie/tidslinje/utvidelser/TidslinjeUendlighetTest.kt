package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.INF
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjeUendlighetTest {
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
    fun `kan legge sammen to tidslinjer, hvor den ene er uendelig`() {
        init(
            listOf(TidslinjePeriode(1, 1, false), TidslinjePeriode(2, INF, true)),
            listOf(TidslinjePeriode(3, 1, false)),
        )

        var t3 =
            t1.biFunksjon(t2) { t1, t2 ->
                if (t1 is Udefinert || t2 is Udefinert) Udefinert() else Verdi(t1.verdi!! + t2.verdi!!)
            }

        assertTrue { t3.innhold.size == 2 }
        assertTrue { t3.innhold.last().erUendelig }

        init(
            listOf(
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(3, 1, false),
                TidslinjePeriode(4, 1, false),
                TidslinjePeriode(5, 1, false),
                TidslinjePeriode(6, 1, false),
                TidslinjePeriode(7, INF, true),
            ),
            listOf(TidslinjePeriode(3, 1, false)),
        )

        t3 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        assertTrue { t3.innhold.size == 2 }
        assertTrue { t3.innhold.last().erUendelig }
    }

    @Test
    fun `kan legge sammen uendelige tidslinjer`() {
        init(
            listOf(TidslinjePeriode(1, 1, false), TidslinjePeriode(2, INF, true)),
            listOf(TidslinjePeriode(3, 1, true)),
        )

        var t3 = t1.biFunksjon(t2) { t1, t2 -> Verdi(t1.verdi!! + t2.verdi!!) }

        assertTrue { t3.innhold.size == 2 }
        assertTrue { t3.innhold.last().erUendelig }

        val t4 = t1.binærOperator(t2) { t1, t2 -> Verdi(t1.verdi!! + t2.verdi!!) }

        assertTrue { t4.innhold.size == 2 }
        assertTrue { t4.innhold.last().erUendelig }

        init(
            listOf(TidslinjePeriode(1, 1, true), TidslinjePeriode(2, INF, false)),
            listOf(TidslinjePeriode(1, 1, true), TidslinjePeriode(3, 1, false)),
        )

        t3 = t1.biFunksjon(t2) { t1, t2 -> Verdi(t1.verdi!! + t2.verdi!!) }

        assertTrue { t3.innhold.size == 1 }
        assertTrue { t3.innhold.last().erUendelig }
    }

    @Test
    fun `kan håndtere uendlighet i midten av en tidlinje`() {
        init(
            listOf(
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(3, 1, true),
                TidslinjePeriode(4, 1, false),
                TidslinjePeriode(5, 1, false),
                TidslinjePeriode(6, 1, false),
                TidslinjePeriode(7, INF, true),
            ),
            listOf(TidslinjePeriode(3, 1, false)),
        )

        assertTrue { t1.innhold.size == 3 }
        assertTrue { t1.innhold.last().erUendelig }

        val t3 =
            t1.biFunksjon(t2) { t1, t2 ->
                if (t1 is Udefinert || t2 is Udefinert) Udefinert() else Verdi(t1.verdi!! + t2.verdi!!)
            }

        assertTrue { t3.innhold.size == 2 }
        assertTrue { t3.innhold.last().erUendelig }
    }
}
