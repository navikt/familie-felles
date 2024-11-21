package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjeEnkelTest {
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
    fun `kan summere og substrahere to tidslinjer bestående av heltall`() {
        init(
            listOf(TidslinjePeriode(1, 1, false), TidslinjePeriode(2, 1, false)),
            listOf(TidslinjePeriode(3, 1, false), TidslinjePeriode(4, 1, false)),
        )

        var t3 = t1.biFunksjon(t2) { t1, t2 -> Verdi(t1.verdi!! + t2.verdi!!) }

        val lst3 = lst1.map { it.periodeVerdi.verdi!! }.toMutableList()
        for (i in 0 until lst3.size) {
            lst3[i] = lst3[i].plus(lst2[i].periodeVerdi.verdi!!)
        }

        Assertions.assertEquals(lst3, t3.innhold.map { it.periodeVerdi.verdi }.toList(), "Kunne ikke addere to tidslinjer")

        t3 = t1.biFunksjon(t2) { t1, t2 -> Verdi(t1.verdi!! - t2.verdi!!) }

        val lst4 = listOf(-2)

        Assertions.assertEquals(lst4, t3.innhold.map { it.periodeVerdi.verdi }.toList(), "Kunne ikke substrahere to tidslinjer")
    }

    @Test
    fun `kan gange to tidslinjer bestående av heltall sammen`() {
        init(
            listOf(TidslinjePeriode(1, 1, false), TidslinjePeriode(2, 1, false)),
            listOf(TidslinjePeriode(3, 1, false), TidslinjePeriode(4, 1, false)),
        )

        val t3 = t1.biFunksjon(t2) { t1, t2 -> Verdi(t1.verdi!! * t2.verdi!!) }

        val lst3 = lst1.map { it.periodeVerdi.verdi!! }.toMutableList()
        for (i in 0 until lst3.size) {
            lst3[i] *= lst2[i].periodeVerdi.verdi!!
        }

        Assertions.assertEquals(lst3, t3.innhold.map { it.periodeVerdi.verdi }.toList(), "Kunne ikke addere to tidslinjer")
    }

    @Test
    fun `kan summere to tidslinjer med ulikt antall TidslinjePerioder`() {
        init(
            listOf(TidslinjePeriode(1, 2, false)),
            listOf(TidslinjePeriode(3, 1, false), TidslinjePeriode(4, 1, false)),
        )

        var t3 = t1.biFunksjon(t2) { t1, t2 -> Verdi(t1.verdi!! + t2.verdi!!) }

        var lst3 = lst2.map { it.periodeVerdi.verdi!! }.toMutableList()
        for (i in 0 until lst3.size) {
            lst3[i] += lst1[0].periodeVerdi.verdi!!
        }

        Assertions.assertEquals(
            lst3,
            t3.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulikt antall TidslinjePerioder: første sett",
        )

        lst1 = listOf(TidslinjePeriode(1, 8, false))
        lst2 = listOf(TidslinjePeriode(3, 4, false), TidslinjePeriode(4, 3, false), TidslinjePeriode(5, 1, false))
        t1 = Tidslinje(LocalDate.now(), lst1)
        t2 = Tidslinje(LocalDate.now(), lst2)

        t3 = t1.biFunksjon(t2) { t1, t2 -> Verdi(t1.verdi!! + t2.verdi!!) }

        lst3 = lst2.map { it.periodeVerdi.verdi!! }.toMutableList()
        for (i in 0 until lst3.size) {
            lst3[i] += lst1[0].periodeVerdi.verdi!!
        }

        Assertions.assertEquals(
            lst3,
            t3.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulikt antall TidslinjePerioder: andre sett",
        )
    }

    @Test
    fun `Ved initsialisering av tidlinjer vil påfølgende TidslinjePerioder med lik verdi slått sammen`() {
        val tidslinjePerioder =
            listOf(
                TidslinjePeriode(1, 6, false),
                TidslinjePeriode(1, 6, false),
                TidslinjePeriode(1, 6, false),
                TidslinjePeriode(1, 6, false),
            )
        val t1 = Tidslinje(LocalDate.now(), tidslinjePerioder)
        Assertions.assertEquals(1, t1.innhold.size)
        Assertions.assertEquals(1, t1.innhold[0].periodeVerdi.verdi)
    }

    @Test
    fun `kan gjøre beregninger med tidslinjer som inneholder store mengder data`() {
        val lst1 = mutableListOf<TidslinjePeriode<Int>>()
        val lst2 = mutableListOf<TidslinjePeriode<Int>>()
        val lst3 = mutableListOf<Int>()
        for (i in 0..1_000_000) {
            lst1.add(TidslinjePeriode(i, 2, false))
            lst2.add(TidslinjePeriode(2 * i, 2, false))
            lst3.add(3 * i)
        }

        val tidslinje1 = Tidslinje(LocalDate.now(), lst1)
        val tidslinje2 = Tidslinje(LocalDate.now(), lst2)

        val kombinertTidslinje = tidslinje1.biFunksjon(tidslinje2) { t1, t2 -> Verdi(t1.verdi!! + t2.verdi!!) }

        Assertions.assertEquals(lst3, kombinertTidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
    }

    @Test
    fun `kan bruke objekter som verdier i tidslinjene`() {
        val tidslinjePerioder1 =
            listOf(
                TidslinjePeriode(Beløp(2.0, "nok"), 1),
                TidslinjePeriode(Beløp(3.0, "nok"), 1),
                TidslinjePeriode(Beløp(3.0, "nok"), 1),
            )
        val tidslinjePerioder2 =
            listOf(
                TidslinjePeriode(Beløp(2.0, "nok"), 1),
                TidslinjePeriode(Beløp(3.0, "nok"), 1),
                TidslinjePeriode(Beløp(4.0, "nok"), 1),
            )

        val tidslinje1 = Tidslinje(LocalDate.now(), tidslinjePerioder1)

        assertTrue { tidslinje1.innhold.size == 2 }

        val tidslinje2 = Tidslinje(LocalDate.now(), tidslinjePerioder2)

        val tidslinje3 = tidslinje1.biFunksjon(tidslinje2) { t1, t2 -> Verdi(Beløp(t1.verdi!!.verdi + t2.verdi!!.verdi, "nok")) }

        val verdierTidslinjePerioder1 = tidslinjePerioder1.map { it.periodeVerdi.verdi!!.verdi }.toMutableList()

        for (i in 0 until verdierTidslinjePerioder1.size) {
            verdierTidslinjePerioder1[i] += tidslinjePerioder2[i].periodeVerdi.verdi!!.verdi
        }

        Assertions.assertEquals(
            verdierTidslinjePerioder1,
            tidslinje3.innhold.map { it.periodeVerdi.verdi!!.verdi }.toList(),
            "Kunne ikke addere to tidslinjer",
        )
    }
}

data class Beløp(
    val verdi: Double,
    val valutaKode: String,
)
