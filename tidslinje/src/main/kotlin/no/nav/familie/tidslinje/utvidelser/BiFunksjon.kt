package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.INF
import no.nav.familie.tidslinje.PeriodeVerdi
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Udefinert
import java.time.LocalDate

/**
 * Beregner en ny tidslinje ved bruk av [kombineringsfunksjon] basert på tidslinjene this og [annen].
 * Går gjennom alle periodene i hver tidslinje og beregner nye perioder.
 * Hver nye periode som blir lagt inn i den resulterende tidslinja starter der den forrige stoppet og
 * har lengde lik det minste tidsrommet hvor input tidslinjene har konstant verdi.
 * Påfølgende perioder med lik verdi, blir slått sammen til en periode i den resulterende tidslinja.
 * [kombineringsfunksjon] blir brukt for å beregne verdiene til de generete periodene basert på verdiene til input tidslinjene i
 * de respektive tidsrommene. Om en av input-tidslinjene er uendleig(dvs at den siste perioden har uendelig varighet), vil
 * også den resulterende tidslinja være uendelig.
 * [PeriodeVerdi] er en wrapper-classe som blir brukt for å håndtere no.nav.familie.tidslinje.Udefinert og no.nav.familie.tidslinje.Null. [kombineringsfunksjon]
 * må ta høyde for at input kan være av en av disse typene, og definere hvordan disse situasjonene håndteres.
 * MERK: operator skal returnere enten Udefindert, no.nav.familie.tidslinje.Null eller no.nav.familie.tidslinje.PeriodeVerdi.
 */
fun <T, R, RESULTAT> Tidslinje<T>.biFunksjon(
    annen: Tidslinje<R>,
    kombineringsfunksjon: (elem1: PeriodeVerdi<T>, elem2: PeriodeVerdi<R>) -> PeriodeVerdi<RESULTAT>,
): Tidslinje<RESULTAT> {
    val tidslinjePerioder: MutableList<TidslinjePeriode<RESULTAT>> = mutableListOf()
    var kopi1 = this
    var kopi2 = annen

    if (this.tidsEnhet != annen.tidsEnhet) {
        kopi1 = this.konverterTilDag()
        kopi2 = annen.konverterTilDag()
    }

    val (kopi3, kopi4) = konverterTilSammeLengde(kopi1, kopi2)

    val it1: Iterator<TidslinjePeriode<T>> = kopi3.innhold.iterator()
    val it2: Iterator<TidslinjePeriode<R>> = kopi4.innhold.iterator()

    var tidslinjePeriode1: TidslinjePeriode<T>? = null
    var tidslinjePeriode2: TidslinjePeriode<R>? = null

    var lengde1 = 0
    var lengde2 = 0

    while (it1.hasNext() || it2.hasNext()) {
        tidslinjePeriode1 = tidslinjePeriode1.takeIf { lengde1 > 0 } ?: it1.next()
        tidslinjePeriode2 = tidslinjePeriode2.takeIf { lengde2 > 0 } ?: it2.next()

        lengde1 = lengde1.takeIf { it > 0 } ?: tidslinjePeriode1.lengde
        lengde2 = lengde2.takeIf { it > 0 } ?: tidslinjePeriode2.lengde

        if (tidslinjePeriode1.erUendelig && tidslinjePeriode2.erUendelig) {
            tidslinjePerioder.add(
                tidslinjePeriode1.biFunksjon(
                    operand = tidslinjePeriode2,
                    lengde = INF,
                    erUendelig = true,
                    operator = kombineringsfunksjon,
                ),
            )
            break
        }

        val minLengde = minOf(lengde1, lengde2)
        tidslinjePerioder.add(
            tidslinjePeriode1.biFunksjon(
                operand = tidslinjePeriode2,
                lengde = minLengde,
                erUendelig = false,
                operator = kombineringsfunksjon,
            ),
        )

        lengde1 -= minLengde
        lengde2 -= minLengde
    }

    val resultatTidslinje = Tidslinje(kopi3.startsTidspunkt, tidslinjePerioder, kopi3.tidsEnhet)

    @Suppress("UNCHECKED_CAST")
    resultatTidslinje.foreldre.add(kopi3 as Tidslinje<Any>)
    @Suppress("UNCHECKED_CAST")
    resultatTidslinje.foreldre.add(kopi4 as Tidslinje<Any>)

    return resultatTidslinje.medTittel(this.tittel)
}

fun <A, B, C, RESULTAT> Tidslinje<A>.biFunksjon(
    tidslinje2: Tidslinje<B>,
    tidslinje3: Tidslinje<C>,
    kombineringsfunksjon: (PeriodeVerdi<A>, PeriodeVerdi<B>, PeriodeVerdi<C>) -> PeriodeVerdi<RESULTAT>,
): Tidslinje<RESULTAT> {
    val tidslinjePerioder: MutableList<TidslinjePeriode<RESULTAT>> = mutableListOf()
    var kopi1 = this
    var kopi2 = tidslinje2
    var kopi3 = tidslinje3

    val erUlikTidsEnhet = this.tidsEnhet != tidslinje2.tidsEnhet || this.tidsEnhet != tidslinje3.tidsEnhet

    if (erUlikTidsEnhet) {
        kopi1 = this.konverterTilDag()
        kopi2 = tidslinje2.konverterTilDag()
        kopi3 = tidslinje3.konverterTilDag()
    }

    val (kopi4, kopi5, kopi6) = konverterTilSammeLengde(kopi1, kopi2, kopi3)

    val it1: Iterator<TidslinjePeriode<A>> = kopi4.innhold.iterator()
    val it2: Iterator<TidslinjePeriode<B>> = kopi5.innhold.iterator()
    val it3: Iterator<TidslinjePeriode<C>> = kopi6.innhold.iterator()

    var tidslinjePeriode1: TidslinjePeriode<A>? = null
    var tidslinjePeriode2: TidslinjePeriode<B>? = null
    var tidslinjePeriode3: TidslinjePeriode<C>? = null

    var lengde1 = 0
    var lengde2 = 0
    var lengde3 = 0

    while (it1.hasNext() || it2.hasNext() || it3.hasNext()) {
        tidslinjePeriode1 = tidslinjePeriode1.takeIf { lengde1 > 0 } ?: it1.next()
        tidslinjePeriode2 = tidslinjePeriode2.takeIf { lengde2 > 0 } ?: it2.next()
        tidslinjePeriode3 = tidslinjePeriode3.takeIf { lengde3 > 0 } ?: it3.next()

        lengde1 = lengde1.takeIf { it > 0 } ?: tidslinjePeriode1.lengde
        lengde2 = lengde2.takeIf { it > 0 } ?: tidslinjePeriode2.lengde
        lengde3 = lengde3.takeIf { it > 0 } ?: tidslinjePeriode3.lengde

        if (tidslinjePeriode1.erUendelig && tidslinjePeriode2.erUendelig && tidslinjePeriode3.erUendelig) {
            tidslinjePerioder.add(
                tidslinjePeriode1.biFunksjon(
                    operand1 = tidslinjePeriode2,
                    operand2 = tidslinjePeriode3,
                    lengde = INF,
                    erUendelig = true,
                    operator = kombineringsfunksjon,
                ),
            )
            break
        }

        val minLengde = minOf(lengde1, lengde2, lengde3)
        tidslinjePerioder.add(
            tidslinjePeriode1.biFunksjon(
                operand1 = tidslinjePeriode2,
                operand2 = tidslinjePeriode3,
                lengde = minLengde,
                erUendelig = false,
                operator = kombineringsfunksjon,
            ),
        )

        lengde1 -= minLengde
        lengde2 -= minLengde
        lengde3 -= minLengde
    }

    val resultatTidslinje = Tidslinje(kopi4.startsTidspunkt, tidslinjePerioder, kopi4.tidsEnhet)

    @Suppress("UNCHECKED_CAST")
    resultatTidslinje.foreldre.add(kopi4 as Tidslinje<Any>)
    @Suppress("UNCHECKED_CAST")
    resultatTidslinje.foreldre.add(kopi5 as Tidslinje<Any>)
    @Suppress("UNCHECKED_CAST")
    resultatTidslinje.foreldre.add(kopi6 as Tidslinje<Any>)

    return resultatTidslinje.medTittel(this.tittel)
}

fun <T, R, RESULTAT> Tidslinje<T>.biFunksjonSnitt(
    operand: Tidslinje<R>,
    operator: (elem1: PeriodeVerdi<T>, elem2: PeriodeVerdi<R>) -> PeriodeVerdi<RESULTAT>,
): Tidslinje<RESULTAT> {
    val startsTidspunkt =
        if (this.startsTidspunkt < operand.startsTidspunkt) operand.startsTidspunkt else this.startsTidspunkt

    val sluttTidspunkt1 = this.kalkulerSluttTidspunkt()
    val sluttTidspunkt2 = operand.kalkulerSluttTidspunkt()

    val sluttTidspunkt = if (sluttTidspunkt1 < sluttTidspunkt2) sluttTidspunkt1 else sluttTidspunkt2

    return this
        .biFunksjon(operand, operator)
        .klipp(startsTidspunkt, sluttTidspunkt)
}

/**
 * Konverterer to input-tidslinjer til å bli av samme lengde. Dette gjør den ved å legge til en "padding" bestående av en periode
 * med lengde lik differansen mellom de to tidspunktene og verdi gitt av [nullVerdi] til tidslinjen.
 * Antar tidslinjene er av samme tidsenhet!!
 */
private fun <A, B> konverterTilSammeLengde(
    tidslinje1: Tidslinje<A>,
    tidslinje2: Tidslinje<B>,
): Pair<Tidslinje<A>, Tidslinje<B>> {
    val kopi1 = tidslinje1.kopier()
    val kopi2 = tidslinje2.kopier()

    val tidligsteStartTidspunkt = listOf(kopi1, kopi2).minOf { it.startsTidspunkt }
    val senesteSluttTidspunkt = listOf(kopi1, kopi2).maxOf { it.kalkulerSluttTidspunkt() }
    val erUendelig = listOf(kopi1, kopi2).any { it.innhold.isNotEmpty() && it.innhold.last().erUendelig }

    settStarttidspunkt(kopi1, tidligsteStartTidspunkt)
    settStarttidspunkt(kopi2, tidligsteStartTidspunkt)

    settSlutttidspunkt(kopi1, senesteSluttTidspunkt, erUendelig)
    settSlutttidspunkt(kopi2, senesteSluttTidspunkt, erUendelig)

    return Pair(kopi1, kopi2)
}

private fun <A, B, C> konverterTilSammeLengde(
    tidslinje1: Tidslinje<A>,
    tidslinje2: Tidslinje<B>,
    tidslinje3: Tidslinje<C>,
): Triple<Tidslinje<A>, Tidslinje<B>, Tidslinje<C>> {
    val kopi1 = tidslinje1.kopier()
    val kopi2 = tidslinje2.kopier()
    val kopi3 = tidslinje3.kopier()

    val tidligsteStartTidspunkt = listOf(kopi1, kopi2, kopi3).minOf { it.startsTidspunkt }
    val senesteSluttTidspunkt = listOf(kopi1, kopi2, kopi3).maxOf { it.kalkulerSluttTidspunkt() }
    val erUendelig = listOf(kopi1, kopi2, kopi3).any { it.innhold.isNotEmpty() && it.innhold.last().erUendelig }

    settStarttidspunkt(kopi1, tidligsteStartTidspunkt)
    settStarttidspunkt(kopi2, tidligsteStartTidspunkt)
    settStarttidspunkt(kopi3, tidligsteStartTidspunkt)

    settSlutttidspunkt(kopi1, senesteSluttTidspunkt, erUendelig)
    settSlutttidspunkt(kopi2, senesteSluttTidspunkt, erUendelig)
    settSlutttidspunkt(kopi3, senesteSluttTidspunkt, erUendelig)

    return Triple(kopi1, kopi2, kopi3)
}

private fun <T> settSlutttidspunkt(
    tidslinje: Tidslinje<T>,
    senesteSluttTidspunkt: LocalDate,
    erUendelig: Boolean,
) {
    val sluttTidspunkt = tidslinje.kalkulerSluttTidspunkt()
    if (sluttTidspunkt < senesteSluttTidspunkt) {
        val lengde =
            if (erUendelig) {
                INF
            } else {
                sluttTidspunkt
                    .until(senesteSluttTidspunkt.plusDays(1), mapper[tidslinje.tidsEnhet])
                    .toInt()
            }
        tidslinje.innhold += listOf(TidslinjePeriode(Udefinert(), lengde, erUendelig))
    }
}

private fun <T> settStarttidspunkt(
    tidslinje: Tidslinje<T>,
    tidligsteStartTidspunkt: LocalDate,
) {
    if (tidslinje.startsTidspunkt > tidligsteStartTidspunkt) {
        val diffFraTidligsteStartTidspunkt =
            tidligsteStartTidspunkt.until(tidslinje.startsTidspunkt, mapper[tidslinje.tidsEnhet]).toInt()
        tidslinje.innhold =
            listOf(TidslinjePeriode(Udefinert<T>(), diffFraTidligsteStartTidspunkt, false)) + tidslinje.innhold
        tidslinje.startsTidspunkt = tidligsteStartTidspunkt
    }
}
