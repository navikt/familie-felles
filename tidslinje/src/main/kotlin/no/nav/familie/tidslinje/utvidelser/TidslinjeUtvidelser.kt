package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.INF
import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.PRAKTISK_SENESTE_DAG
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.PeriodeVerdi
import no.nav.familie.tidslinje.TidsEnhet
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.TidslinjePeriodeMedDato
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilPeriodeVerdi
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val TIDENES_ENDE = LocalDate.MAX

val mapper =
    mapOf(
        TidsEnhet.ÅR to ChronoUnit.YEARS,
        TidsEnhet.MÅNED to ChronoUnit.MONTHS,
        TidsEnhet.UKE to ChronoUnit.WEEKS,
        TidsEnhet.DAG to ChronoUnit.DAYS,
    )

fun <T> Tidslinje<T>.medTittel(tittel: String): Tidslinje<T> {
    this.tittel = tittel
    return this
}

/**
 * Tar inn en tidslinje av en vilkårlig tidsenhet (sålangt kan det kun være MÅNED eller DAG) og returnerer den som DAG.
 */
fun <T> Tidslinje<T>.konverterTilDag(): Tidslinje<T> {
    if (this.tidsEnhet == TidsEnhet.DAG) return this

    var tidspunkt = this.startsTidspunkt

    when (this.tidsEnhet) {
        TidsEnhet.UKE -> {
            for (periode in this.innhold) {
                val nyttTidspunkt = tidspunkt.plusWeeks(periode.lengde.toLong())
                periode.lengde = tidspunkt.until(nyttTidspunkt, ChronoUnit.DAYS).toInt()
                tidspunkt = nyttTidspunkt
            }
        }

        TidsEnhet.MÅNED -> {
            for (periode in this.innhold) {
                val nyttTidspunkt = tidspunkt.plusMonths(periode.lengde.toLong())
                periode.lengde = tidspunkt.until(nyttTidspunkt, ChronoUnit.DAYS).toInt()
                tidspunkt = nyttTidspunkt
            }
        }

        else -> {
            for (periode in this.innhold) {
                val nyttTidspunkt = tidspunkt.plusYears(periode.lengde.toLong())
                periode.lengde = tidspunkt.until(nyttTidspunkt, ChronoUnit.DAYS).toInt()
                tidspunkt = nyttTidspunkt
            }
        }
    }

    this.tidsEnhet = TidsEnhet.DAG

    return this
}

/**
 * Tar inn en operator som bestemmer hvordan periodeverdiene i tidslinja skal mappes til andre periodeverdier.
 * [operator] er en funksjon som tar inn en periodeverdi<T> og returnerer en periodeverdi<R>.
 * Dette åpner for at man kan mappe no.nav.familie.tidslinje.Null og no.nav.familie.tidslinje.Udefinert objekter til andre verdier.
 */
fun <T, R> Tidslinje<T>.map(operator: (elem: PeriodeVerdi<T>) -> PeriodeVerdi<R>): Tidslinje<R> {
    val perioder = mutableListOf<TidslinjePeriode<R>>()

    this.innhold.forEach { perioder.add(TidslinjePeriode(operator(it.periodeVerdi), it.lengde, it.erUendelig)) }

    val tidslinje = Tidslinje(this.startsTidspunkt, perioder, this.tidsEnhet)

    tidslinje.foreldre.addAll(this.foreldre)

    return tidslinje
        .medTittel(this.tittel)
}

/**
 * Fjerner alle periodeverdier i [periodeVerdier] fra slutten og begynnelsen av tidslinja.
 * Ved fjerning av perioder i begynnelsen av tislinja, vil startspunktet bli flyttet.
 */
fun <T> Tidslinje<T>.trim(vararg periodeVerdier: PeriodeVerdi<T>): Tidslinje<T> =
    this
        .trimVenstre(*periodeVerdier)
        .trimHøyre(*periodeVerdier)
        .medTittel(this.tittel)

fun <T> Tidslinje<T>.trimVenstre(vararg periodeVerdier: PeriodeVerdi<T>): Tidslinje<T> {
    val perioder = ArrayList(this.innhold)
    var startsTidspunkt = this.startsTidspunkt

    for (periode in this.innhold) {
        if (periodeVerdier.contains(periode.periodeVerdi)) {
            startsTidspunkt = startsTidspunkt.plus(periode.lengde.toLong(), mapper[this.tidsEnhet])
            perioder.remove(periode)
        } else {
            break
        }
    }
    val resultat = Tidslinje(startsTidspunkt, perioder, this.tidsEnhet)

    this.foreldre.forEach {
        if (!resultat.foreldre.contains(it)) {
            resultat.foreldre.add(it)
        }
    }

    return resultat
}

fun <T> Tidslinje<T>.trimHøyre(vararg periodeVerdier: PeriodeVerdi<T>): Tidslinje<T> {
    val perioder = ArrayList(this.innhold)

    for (periode in this.innhold.reversed()) {
        if (periodeVerdier.contains(periode.periodeVerdi)) {
            perioder.remove(periode)
        } else {
            break
        }
    }

    val resultat = Tidslinje(this.startsTidspunkt, perioder, this.tidsEnhet)

    this.foreldre.forEach {
        if (!resultat.foreldre.contains(it)) {
            resultat.foreldre.add(it)
        }
    }

    return resultat
}

/**
 * Tar inn en tidslinje med tidsenhet DAG og returnerer en med tidsenhet MÅNED.
 * Parameteren [vindu] bestemmer hvor mange måneder som skal bli tatt med i beregeningen av månedsverdien for gjeldene måned.
 * Med vindu parameteren satt for man en sliding-window effekt med steglende en.
 * Operator definerer hvordan verdiene for alle månedene skal bli valgt (f.eks. skal det tas gjennomsnitt av alle verdiene
 * innad i den måneden, skal den største verdien velges eller skal den siste verdien velges).
 * [dato] parameteren i operator oppdateres til å være første dag i gjeldene måned under beregningene.
 */
fun <T> Tidslinje<T>.konverterTilMåned(
    antallMndBakoverITid: Int = 0,
    antallMndFremoverITid: Int = 0,
    operator: (dato: LocalDate, månedListe: List<List<TidslinjePeriode<T>>>) -> PeriodeVerdi<T>,
): Tidslinje<T> {
    val listeAvMåneder: MutableList<List<TidslinjePeriode<T>>> = this.splittPåMåned()

    if (listeAvMåneder.size < antallMndBakoverITid + antallMndFremoverITid) {
        throw java.lang.IllegalArgumentException("Det er for få månender i tidslinja for dette vinduet.")
    }

    listeAvMåneder.addAll(0, (0 until antallMndBakoverITid).map { emptyList() })
    listeAvMåneder.addAll((0 until antallMndFremoverITid).map { emptyList() })

    val perioder: MutableList<TidslinjePeriode<T>> = mutableListOf()
    var dato = this.startsTidspunkt.withDayOfMonth(1)

    listeAvMåneder.windowed(size = antallMndBakoverITid + antallMndFremoverITid + 1, partialWindows = false) { vindu ->
        perioder.add(TidslinjePeriode(operator(dato, vindu), 1, false))
        dato = dato.plusMonths(1)
        // dersom inneværende måned er uendelig, må man beregne verdien dersom vinduet kun dekker den uendelige periodeVerdien.
        if (vindu[antallMndBakoverITid].last().erUendelig) {
            val listeMedUendeligPeriodeVerdier =
                (0..antallMndBakoverITid + antallMndFremoverITid).map { listOf(vindu[antallMndBakoverITid].last()) }
            perioder.add(TidslinjePeriode(operator(dato, listeMedUendeligPeriodeVerdier), INF, true))
        }
    }

    return Tidslinje(this.startsTidspunkt, perioder, TidsEnhet.MÅNED)
        .medTittel(this.tittel)
}

/**
 * Shifter en tidslinje [antall] tidsenheter (enten DAG eller MÅNED) mot høyre.
 */
fun <T> Tidslinje<T>.høyreShift(antall: Int = 1): Tidslinje<T> =
    Tidslinje(this.startsTidspunkt.plus(antall.toLong(), mapper[this.tidsEnhet]), this.innhold, this.tidsEnhet)
        .medTittel(this.tittel)

/**
 * Splitter periodene i en tidslinje på månedsgrenser og returnerer en liste av
 * lister med perioder der hver liste representerer en egen måned.
 */
fun <T> Tidslinje<T>.splittPåMåned(): MutableList<List<TidslinjePeriode<T>>> {
    var nåværendeMåned = this.startsTidspunkt
    var antallDagerIgjenIMåned = this.startsTidspunkt.lengthOfMonth() - this.startsTidspunkt.dayOfMonth + 1

    var månedListe: MutableList<TidslinjePeriode<T>> = mutableListOf() // representerer periodene innad i en måned

    val listeAvMåneder: MutableList<List<TidslinjePeriode<T>>> =
        mutableListOf() // liste av lister som representerer en måned

    this.innhold.forEachIndexed { index, periode ->

        if (periode.erUendelig) { // Her håndteres uendelige perioder
            // om vi er midt i en måned må vi først legge til en ikke-uendelig periode som fyller opp denne
            if (nåværendeMåned.lengthOfMonth() > antallDagerIgjenIMåned) {
                månedListe.add(TidslinjePeriode(periode.periodeVerdi, antallDagerIgjenIMåned, false))
                listeAvMåneder.add(månedListe)
                månedListe = mutableListOf()
                nåværendeMåned = nåværendeMåned.plusMonths(1)
            }
            månedListe.add(periode) // her adder vi den uendelige perioden
            listeAvMåneder.add(månedListe) // om koden har kommet hit vil den være ferdig, da uendelige perioder alltid er bakerst
        } else if (periode.lengde < antallDagerIgjenIMåned) { // Her håndteres perioder som går opp i inneværende måned
            månedListe.add(periode)
            antallDagerIgjenIMåned -= periode.lengde

            if (index + 1 == this.innhold.size) { // om vi er på siste periode er vi ferdige og månedListe kan addes.
                listeAvMåneder.add(månedListe)
            }
        } else { // Her håndteres perioder som er lenger enn inneværende måned og ikke uendelig
            var lengde = periode.lengde // holder oversikt over hvor mange dager vi har igjen av en gitt periode

            while (lengde > antallDagerIgjenIMåned) { // "Så lenge perioden har igjen flere dager enn det er dager igjen i måneden"
                månedListe.add(TidslinjePeriode(periode.periodeVerdi, antallDagerIgjenIMåned))
                listeAvMåneder.add(månedListe)
                månedListe = mutableListOf()
                lengde -= antallDagerIgjenIMåned
                nåværendeMåned = nåværendeMåned.plusMonths(1)
                antallDagerIgjenIMåned = nåværendeMåned.lengthOfMonth()
            }
            // kommer koden hit betyr det at gjenstående lengde til perioden er mindre enn antallDagerIgjenIMåned
            if (lengde > 0) {
                månedListe.add(TidslinjePeriode(periode.periodeVerdi, lengde))
                antallDagerIgjenIMåned -= lengde

                if (antallDagerIgjenIMåned == 0) {
                    listeAvMåneder.add(månedListe)
                    månedListe = mutableListOf()
                    nåværendeMåned = nåværendeMåned.plusMonths(1)
                    antallDagerIgjenIMåned = nåværendeMåned.lengthOfMonth()
                } else if (index + 1 == this.innhold.size) { // om vi er på siste periode er vi ferdige og månedListe kan addes.
                    listeAvMåneder.add(månedListe)
                }
            }
        }
    }
    return listeAvMåneder
}

private fun <R> klippeOperator(
    status1: PeriodeVerdi<R>,
    status2: PeriodeVerdi<Boolean>,
): PeriodeVerdi<R> =
    if (status1 is Udefinert || status2 is Udefinert) {
        Udefinert()
    } else if (status1 is Null || status2 is Null) {
        Null()
    } else {
        if (status2.verdi == true) {
            status1
        } else {
            Udefinert()
        }
    }

fun <T> Tidslinje<T>.klipp(
    startsTidspunkt: LocalDate,
    sluttTidspunkt: LocalDate,
): Tidslinje<T> {
    val foreldre = this.foreldre

    var resultat =
        if (sluttTidspunkt.isAfter(startsTidspunkt)) {
            val justertSluttTidspunkt =
                if (sluttTidspunkt == TIDENES_ENDE) sluttTidspunkt else sluttTidspunkt.plusDays(1)

            val tidslinjeKlipp =
                Tidslinje(
                    startsTidspunkt,
                    listOf(
                        TidslinjePeriode(
                            true,
                            lengde =
                                startsTidspunkt
                                    .until(
                                        justertSluttTidspunkt,
                                        mapper[this.tidsEnhet],
                                    ).toInt(),
                        ),
                    ),
                    this.tidsEnhet,
                )
            this
                .biFunksjon(tidslinjeKlipp) { status1, status2 ->
                    klippeOperator(
                        status1,
                        status2,
                    )
                }.fjernForeldre()
        } else {
            Tidslinje(startsTidspunkt, emptyList(), this.tidsEnhet)
        }

    resultat = resultat.trim(Udefinert())

    if (resultat.startsTidspunkt > sluttTidspunkt) {
        resultat.startsTidspunkt = startsTidspunkt
    }

    resultat.foreldre.addAll(foreldre)
    return resultat
        .medTittel(this.tittel)
}

/**
 * Beregner en ny tidslinje ved bruk av [operator] basert på tidslinjene this og [operand].
 * Går gjennom alle periodene i hver tidslinje og beregner nye perioder.
 * Hver nye periode som blir lagt inn i den resulterende tidslinja starter der den forrige stoppet og
 * har lengde lik det minste tidsrommet hvor input tidslinjene har konstant verdi.
 * Påfølgende perioder med lik verdi, blir slått sammen til en periode i den resulterende tidslinja.
 * [Operator] blir brukt for å beregne verdiene til de generete periodene basert på verdiene til input tidslinjene i
 * de respektive tidsrommene. Om en av input-tidslinjene er uendleig(dvs at den siste perioden har uendelig varighet), vil
 * også den resulterende tidslinja være uendelig.
 * Denne metoden krever at de to input tidslinjene og den resulterende tidslinja består av samme type verdier T.
 */
fun <T> Tidslinje<T>.binærOperator(
    operand: Tidslinje<T>,
    operator: (elem1: PeriodeVerdi<T>, elem2: PeriodeVerdi<T>) -> PeriodeVerdi<T>,
): Tidslinje<T> = this.biFunksjon(operand, operator)

/**
 * Lager en ny tidslengde fra en streng [innhold] og en mapper [mapper].
 * Mappere må inneholde en mapping for enhver char i strengen til et annet objekt av typen [R].
 * [tidsEnhet] spesifiserer hvor lang tid hver char skal vare.
 * Periodene i den resulterende tidslinja vil ha lengder angitt i dager.
 * Tidslunja starter på [startsTidspunkt]
 */
fun <R> Tidslinje.Companion.lagTidslinjeFraStreng(
    innhold: String,
    startDato: LocalDate,
    mapper: Map<Char, R>,
    tidsEnhet: TidsEnhet,
): Tidslinje<R> {
    val lst: MutableList<TidslinjePeriode<R>> = mutableListOf()

    innhold.forEach {
        if (mapper[it] == null) {
            throw java.lang.IllegalArgumentException("Kunne ikke mappe fra char $it til et objekt")
        }
        lst.add(TidslinjePeriode(mapper[it], 1, false))
    }

    return Tidslinje(startDato, lst, tidsEnhet)
}

/**
 * Initialiserer en tidslinje fra listen [innhold].
 * [tidsEnhet] spesifiserer hvor lange periodene for hvert element varer.
 * Periodene i den resulterende tidslinja vil ha lengder angitt i dager.
 * Tidslinja starter på [startsTidspunkt] og behandler [nullVerdi] som nullverdi.
 */
fun <R> Tidslinje.Companion.lagTidslinjeFraListe(
    innhold: List<R?>,
    startDato: LocalDate,
    tidsEnhet: TidsEnhet,
): Tidslinje<R> {
    val perioder =
        innhold.map {
            TidslinjePeriode(it, 1, false)
        }

    return Tidslinje(startDato, perioder, tidsEnhet = tidsEnhet)
}

/**
 * Slår sammen en liste av tidslinjer med samme type ved å ta i bruk reduce. [operator] bestemmer regelen som skal brukes
 * når man slår sammen to liste-elementer. [nullVerdi] må sendes inn fordi denne trengs av binærOperator.
 */
fun <T> List<Tidslinje<T>>.slåSammenLikeTidslinjer(
    operator: (elem1: PeriodeVerdi<T>, elem2: PeriodeVerdi<T>) -> PeriodeVerdi<T>,
): Tidslinje<T> {
    if (this.isEmpty()) {
        throw java.lang.IllegalArgumentException("Lista kan ikke være tom")
    }

    val resultatTidslinje: Tidslinje<T>

    if (this.size == 1) {
        resultatTidslinje = Tidslinje(this[0].startsTidspunkt, this[0].innhold, this[0].tidsEnhet)
        @Suppress("UNCHECKED_CAST")
        resultatTidslinje.foreldre.add(this[0] as Tidslinje<Any>)
    } else {
        resultatTidslinje =
            this.reduce { t1, t2 -> t1.binærOperator(t2) { elem1, elem2 -> operator(elem1, elem2) } }.fjernForeldre()
        resultatTidslinje.foreldre.addAll(this.filterIsInstance<Tidslinje<Any>>())
    }

    return resultatTidslinje
}

fun <T> Tidslinje<T>.fjernForeldre(): Tidslinje<T> {
    this.foreldre.clear()
    return this
}

fun <T> Tidslinje<T>.hentVerdier(): List<T?> = this.innhold.slåSammenLike().map { it.periodeVerdi.verdi }

/**
 * Summerer opp tiden for hver periode og legger inn i en TidslinjePeriodeMedDato
 *
 * Om vi har en tidslinje med starttidspunkt 1. januar og tre perioder som varer én månde hver med verdiene a, b og c
 * vil resulatet bli:
 *
 * List(TidslinjePeriodeMedDato(
 *  verdi: a
 *  fom: 1. januar
 *  tom: 31. januar
 * )
 * TidslinjePeriodeMedDato(
 *  verdi: b
 *  fom: 1. februar
 *  tom: 28. februar
 * )
 * TidslinjePeriodeMedDato(
 *  verdi: c
 *  fom: 1. mars
 *  tom: 31. mars
 * ))
 *
 */
fun <T> Tidslinje<T>.tilTidslinjePerioderMedDato(): List<TidslinjePeriodeMedDato<T>> {
    val (tidslinjePeriodeMedLocalDateListe, _) =
        this.innhold.fold(Pair(emptyList<TidslinjePeriodeMedDato<T>>(), 0L)) {
            (
                tidslinjePeriodeMedLocalDateListe: List<TidslinjePeriodeMedDato<T>>,
                tidFraStarttidspunktFom: Long,
            ),
            tidslinjePeriode,
            ->
            val tidFraStarttidspunktTilNesteFom = tidFraStarttidspunktFom + tidslinjePeriode.lengde

            Pair(
                tidslinjePeriodeMedLocalDateListe +
                    TidslinjePeriodeMedDato(
                        periodeVerdi = tidslinjePeriode.periodeVerdi,
                        fom =
                            TidslinjePeriodeMedDato.Dato(
                                this.startsTidspunkt.leggTil(
                                    tidsEnhet,
                                    tidFraStarttidspunktFom,
                                ),
                            ),
                        tom =
                            if (tidslinjePeriode.erUendelig) {
                                TidslinjePeriodeMedDato.Dato(PRAKTISK_SENESTE_DAG)
                            } else {
                                TidslinjePeriodeMedDato.Dato(
                                    this.startsTidspunkt
                                        .leggTil(tidsEnhet, tidFraStarttidspunktTilNesteFom)
                                        .minusDays(1),
                                )
                            },
                    ),
                tidFraStarttidspunktTilNesteFom,
            )
        }
    return tidslinjePeriodeMedLocalDateListe
}

private fun LocalDate.leggTil(
    tidsEnhet: TidsEnhet,
    antall: Long,
): LocalDate =
    when (tidsEnhet) {
        TidsEnhet.DAG -> this.plusDays(antall)
        TidsEnhet.UKE -> this.plusWeeks(antall)
        TidsEnhet.MÅNED -> this.plusMonths(antall)
        TidsEnhet.ÅR -> this.plusYears(antall)
    }

fun <T> Tidslinje<T>.tilPerioder(): List<Periode<T?>> = this.tilTidslinjePerioderMedDato().map { it.tilPeriode() }

fun <T> Tidslinje<T>.tilPerioderIkkeNull(): List<Periode<T & Any>> = this.tilPerioder().filtrerIkkeNull()

fun <T> Tidslinje<T>.slåSammenLikePerioder(): Tidslinje<T> =
    Tidslinje(
        startsTidspunkt = this.startsTidspunkt,
        perioder = this.innhold.slåSammenLike(),
        tidsEnhet = this.tidsEnhet,
    )
