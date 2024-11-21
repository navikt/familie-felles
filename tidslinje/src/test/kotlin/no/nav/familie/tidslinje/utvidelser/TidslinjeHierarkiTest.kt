package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjeHierarkiTest {
    private var lst1 = emptyList<TidslinjePeriode<Int>>()
    private var lst2 = emptyList<TidslinjePeriode<Int>>()
    private var lst3 = emptyList<TidslinjePeriode<Int>>()

    private var t1: Tidslinje<Int> = Tidslinje(LocalDate.now(), emptyList())
    private var t2: Tidslinje<Int> = Tidslinje(LocalDate.now(), emptyList())
    private var t3: Tidslinje<Int> = Tidslinje(LocalDate.now(), emptyList())

    private fun init(
        lst1: List<TidslinjePeriode<Int>>,
        lst2: List<TidslinjePeriode<Int>>,
        lst3: List<TidslinjePeriode<Int>>,
    ) {
        this.lst1 = lst1
        this.lst2 = lst2
        this.lst3 = lst3
        this.t1 = Tidslinje(LocalDate.now(), lst1)
        this.t2 = Tidslinje(LocalDate.now(), lst2)
        this.t3 = Tidslinje(LocalDate.now(), lst3)
    }

    @Test
    fun `kan legge sammen lister og par med tidslinjer og danne et hierarkisk system for hvordan en tidslinje er bygget opp`() {
        init(
            listOf(TidslinjePeriode(5, 1, false), TidslinjePeriode(3, 1, false)),
            listOf(TidslinjePeriode(2, 1, false), TidslinjePeriode(1, 1, false)),
            listOf(TidslinjePeriode(3, 1, false), TidslinjePeriode(2, 1, false)),
        )
        var tidslinjeListe = listOf(t1, t2, t3)
        val t4 =
            tidslinjeListe.slåSammenLikeTidslinjer { t1, t2 -> if (t1 is Null || t2 is Null) Null() else Verdi(t1.verdi!! + t2.verdi!!) }

        Assertions.assertEquals(3, t4.foreldre.size)
        Assertions.assertEquals(tidslinjeListe.filterIsInstance<Tidslinje<Any>>(), t4.foreldre)

        val t5 = Tidslinje(LocalDate.now(), listOf(TidslinjePeriode(5, 3, false), TidslinjePeriode(3, 1, false)))

        tidslinjeListe = listOf(t4, t5)

        val t6 =
            tidslinjeListe.slåSammenLikeTidslinjer { t1, t2 ->
                if (t1 is Udefinert || t2 is Udefinert) {
                    Udefinert()
                } else if (t1 is Null || t2 is Null) {
                    Null()
                } else {
                    Verdi(t1.verdi!! + t2.verdi!!)
                }
            }

        Assertions.assertEquals(2, t6.foreldre.size)
        Assertions.assertEquals(t4.foreldre, t6.foreldre[0].foreldre)
        assertTrue(t5.foreldre.isEmpty())
    }

    @Test
    fun `lagrer ikke foreldre dersom de fjernes`() {
        init(
            listOf(TidslinjePeriode(5, 1, false), TidslinjePeriode(3, 1, false)),
            listOf(TidslinjePeriode(2, 1, false), TidslinjePeriode(1, 1, false)),
            listOf(TidslinjePeriode(3, 1, false), TidslinjePeriode(2, 1, false)),
        )
        val t5 = t1.biFunksjon(t2) { t1, t2 -> if (t1 is Null || t2 is Null) Null() else Verdi(t1.verdi!! - t2.verdi!!) }
        t5.fjernForeldre()

        assertTrue(t5.foreldre.isEmpty())
    }

    @Test
    fun `lagrer foreldre dersom leggTilForeldre-flagget er satt til true (default)`() {
        init(
            listOf(TidslinjePeriode(5, 1, false), TidslinjePeriode(3, 1, false)),
            listOf(TidslinjePeriode(2, 1, false), TidslinjePeriode(1, 1, false)),
            listOf(TidslinjePeriode(3, 1, false), TidslinjePeriode(2, 1, false)),
        )
        val t5 = t1.biFunksjon(t2) { t1, t2 -> if (t1 is Null || t2 is Null) Null() else Verdi(t1.verdi!! - t2.verdi!!) }

        val tidslinjeListe = listOf(t1, t2)

        Assertions.assertEquals(2, t5.foreldre.size)
        Assertions.assertEquals(tidslinjeListe.filterIsInstance<Tidslinje<Any>>(), t5.foreldre)
    }
}
