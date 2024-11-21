package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.TidsEnhet
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjeLengdeTest {
    private var lst1 = emptyList<TidslinjePeriode<Int?>>()
    private var lst2 = emptyList<TidslinjePeriode<Int?>>()

    private var t1: Tidslinje<Int?> = Tidslinje(LocalDate.now(), emptyList())
    private var t2: Tidslinje<Int?> = Tidslinje(LocalDate.now(), emptyList())

    private fun initSammeStartdato(
        lst1: List<TidslinjePeriode<Int?>>,
        lst2: List<TidslinjePeriode<Int?>>,
    ) {
        this.lst1 = lst1
        this.lst2 = lst2
        this.t1 = Tidslinje(LocalDate.now(), lst1)
        this.t2 = Tidslinje(LocalDate.now(), lst2)
    }

    private fun initForskjelligStartdato(
        lst1: List<TidslinjePeriode<Int?>>,
        lst2: List<TidslinjePeriode<Int?>>,
    ) {
        this.lst1 = lst1
        this.lst2 = lst2
        this.t1 = Tidslinje(LocalDate.now(), lst1)
        this.t2 = Tidslinje(LocalDate.now().minusDays(3), lst2)
    }

    @Test
    fun `kan håndtere to tidslinjer med ulik slutt`() {
        initSammeStartdato(
            listOf(TidslinjePeriode(1, 3, false), TidslinjePeriode(2, 1, false)),
            listOf(TidslinjePeriode(3, 1, false), TidslinjePeriode(4, 1, false)),
        )

        // Her testes det med nullverdi lik -1 for bifunksjon, dvs hvis en av tidslinjene er null i en sammenligning vil den returnere null.
        val t3 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        val lst3 = mutableListOf(4, 5, null)

        Assertions.assertEquals(
            lst3,
            t3.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulik slutt",
        )

        val t4 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! - el2.verdi!!)
                }
            }

        val lst4 = mutableListOf(-2, -3, null)

        Assertions.assertEquals(
            lst4,
            t4.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke substrahere to tidslinjer med ulik slutt",
        )
    }

    @Test
    fun `kan håndtere to tidslinjer med ulik startdato`() {
        initForskjelligStartdato(
            listOf(TidslinjePeriode(1, 1, false), TidslinjePeriode(2, 1, false)),
            listOf(TidslinjePeriode(10, 3, false), TidslinjePeriode(3, 1, false), TidslinjePeriode(4, 1, false)),
        )

        val t5 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        val lst5 = mutableListOf(null, 4, 6)

        Assertions.assertEquals(
            lst5,
            t5.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulik slutt",
        )

        val t6 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! - el2.verdi!!)
                }
            }

        val lst6 = mutableListOf(null, -2)

        Assertions.assertEquals(
            lst6,
            t6.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke substrahere to tidslinjer med ulik slutt",
        )
    }

    @Test
    fun `kan håndtere to tidslinjer med ulik start- OG sluttdato`() {
        initForskjelligStartdato(
            listOf(TidslinjePeriode(1, 1, false), TidslinjePeriode(2, 1, false), TidslinjePeriode(15, 4, false)),
            listOf(TidslinjePeriode(10, 3, false), TidslinjePeriode(3, 1, false), TidslinjePeriode(4, 1, false)),
        )

        val t7 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        val lst7 = mutableListOf(null, 4, 6, null)

        Assertions.assertEquals(
            lst7,
            t7.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulik slutt",
        )

        val t8 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! - el2.verdi!!)
                }
            }

        val lst8 = mutableListOf(null, -2, null)

        Assertions.assertEquals(
            lst8,
            t8.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke substrahere to tidslinjer med ulik slutt",
        )
    }

    @Test
    fun `kan håndtere to tidslinjer med ulik start- OG sluttdato OG nullinput`() {
        initForskjelligStartdato(
            listOf(TidslinjePeriode(null, 1, false), TidslinjePeriode(2, 1, false), TidslinjePeriode(15, 4, false)),
            listOf(TidslinjePeriode(10, 3, false), TidslinjePeriode(3, 1, false), TidslinjePeriode(null, 1, false)),
        )

        val t7 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else if (el1 is Null || el2 is Null) {
                    Null()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        val lst7 = mutableListOf(null, null, null)

        Assertions.assertEquals(
            lst7,
            t7.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulik slutt",
        )

        val t8 =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else if (el1 is Null || el2 is Null) {
                    Null()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        val lst8 = mutableListOf(null, null, null)

        Assertions.assertEquals(
            lst8,
            t8.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke substrahere to tidslinjer med ulik slutt",
        )
    }

    @Test
    fun `kan håndtere to tidslinjer med ulik start- og sluttdato på månedsnivå`() {
        val t1 =
            Tidslinje(
                startsTidspunkt = LocalDate.now(),
                perioder =
                    listOf(
                        TidslinjePeriode(1, 1, false),
                        TidslinjePeriode(2, 1, false),
                        TidslinjePeriode(15, 5, false),
                    ),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        val t2 =
            Tidslinje(
                startsTidspunkt = LocalDate.now().minusMonths(2),
                perioder =
                    listOf(
                        TidslinjePeriode(1, 1, false),
                        TidslinjePeriode(2, 1, false),
                        TidslinjePeriode(15, 4, false),
                    ),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        val resultat =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        val fasit = mutableListOf(null, 16, 17, 30, null)

        Assertions.assertEquals(
            fasit,
            resultat.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulik slutt på månedsnivå",
        )
        Assertions.assertEquals(resultat.tidsEnhet, TidsEnhet.MÅNED)
        val endDate = LocalDate.now().plusMonths(6)
        Assertions.assertEquals(endDate.withDayOfMonth(endDate.lengthOfMonth()), resultat.kalkulerSluttTidspunkt())
    }

    @Test
    fun `kan håndtere to tidslinjer på mpnednivå med ulik start og sluttidspunkt`() {
        val t1 =
            Tidslinje(
                startsTidspunkt = LocalDate.of(2022, 2, 1),
                perioder =
                    listOf(
                        TidslinjePeriode(1, 1, false),
                        TidslinjePeriode(2, 1, false),
                        TidslinjePeriode(15, 2, false),
                    ),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        val t2 =
            Tidslinje(
                startsTidspunkt = LocalDate.of(2022, 2, 1),
                perioder =
                    listOf(
                        TidslinjePeriode(1, 1, false),
                        TidslinjePeriode(2, 1, false),
                        TidslinjePeriode(15, 11, false),
                    ),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        val resultat =
            t1.biFunksjon(t2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        val fasit = mutableListOf(2, 4, 30, null)

        Assertions.assertEquals(
            fasit,
            resultat.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulik slutt på månedsnivå",
        )
        Assertions.assertEquals(resultat.tidsEnhet, TidsEnhet.MÅNED)

        val endDate = LocalDate.of(2022, 2, 1).plusMonths(12)
        Assertions.assertEquals(endDate.withDayOfMonth(endDate.lengthOfMonth()), resultat.kalkulerSluttTidspunkt())
    }
}
