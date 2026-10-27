@file:Suppress("UNCHECKED_CAST")

package no.nav.familie.tidslinje

import no.nav.familie.tidslinje.utvidelser.tilTidslinjePerioderMedDato
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Kompakt notasjon for tidslinjer i tester. Hvert tegn er én måned ([mnd]) eller én dag ([dag]),
 * regnet fra [START] (1. januar 2020).
 *
 * | Tegn       | Betydning                                                                   |
 * |------------|-----------------------------------------------------------------------------|
 * | `0`–`9`    | Verdi (Int) i [mnd]/[dag]                                                   |
 * | `A`–`Z`    | Verdi (Char) i [mndTegn]/[dagTegn]                                          |
 * | `n`        | Null: vi vet at det ikke finnes noen verdi                                  |
 * | `-`        | Udefinert: hull. Ledende `-` betyr at tidslinjen starter senere             |
 * | `<` først  | Uendelig start: første tegn gjelder fra [PRAKTISK_TIDLIGSTE_DAG]            |
 * | `>` sist   | Uendelig slutt: siste tegn gjelder uten slutt                               |
 * | tom streng | Tom tidslinje                                                               |
 *
 * Eksempel: `"-12n>"` starter i februar 2020 med 1, har 2 i mars, og er Null fra april og uten slutt.
 *
 * [somMnd] og [somDag] gjør en tidslinje om til samme notasjon, i normalisert form: en uendelig slutt
 * skrives som ett tegn fulgt av `>`, og en uendelig start som `<` fulgt av ett tegn per enhet til og med
 * siste enhet i perioden.
 */
val START: LocalDate = LocalDate.of(2020, 1, 1)

/** Første dag i måned nummer [indeks], regnet fra januar 2020. */
fun mnd(indeks: Int): LocalDate = START.plusMonths(indeks.toLong())

/** Siste dag i måned nummer [indeks], regnet fra januar 2020. */
fun mndSlutt(indeks: Int): LocalDate = mnd(indeks + 1).minusDays(1)

/** Dag nummer [indeks], regnet fra 1. januar 2020. */
fun dag(indeks: Int): LocalDate = START.plusDays(indeks.toLong())

fun String.mnd(): Tidslinje<Int> = tilTidslinje(Enhet.MÅNED) { it.tilTall() }

fun String.dag(): Tidslinje<Int> = tilTidslinje(Enhet.DAG) { it.tilTall() }

fun String.mndTegn(): Tidslinje<Char> = tilTidslinje(Enhet.MÅNED) { it.tilBokstav() }

fun String.dagTegn(): Tidslinje<Char> = tilTidslinje(Enhet.DAG) { it.tilBokstav() }

fun <T> Tidslinje<T>.somMnd(tegn: (T & Any) -> Char = ::standardTegn): String = somStreng(Enhet.MÅNED, tegn)

fun <T> Tidslinje<T>.somDag(tegn: (T & Any) -> Char = ::standardTegn): String = somStreng(Enhet.DAG, tegn)

private enum class Enhet(
    val chronoUnit: ChronoUnit,
) {
    MÅNED(ChronoUnit.MONTHS),
    DAG(ChronoUnit.DAYS),
    ;

    fun fom(indeks: Int): LocalDate = START.plus(indeks.toLong(), chronoUnit)

    fun tom(indeks: Int): LocalDate = fom(indeks + 1).minusDays(1)

    fun indeksForFom(dato: LocalDate): Int {
        val indeks = chronoUnit.between(START, dato).toInt()
        require(fom(indeks) == dato) { "$dato er ikke starten på en ${name.lowercase()}. Bruk somDag." }
        return indeks
    }

    fun indeksForTom(dato: LocalDate): Int {
        val indeks = chronoUnit.between(START, dato.plusDays(1)).toInt() - 1
        require(tom(indeks) == dato) { "$dato er ikke slutten på en ${name.lowercase()}. Bruk somDag." }
        return indeks
    }
}

private const val MAKS_LENGDE = 1000

private fun Char.tilTall(): Int {
    require(this in '0'..'9') { "Ugyldig tegn '$this' i tall-tidslinje" }
    return this - '0'
}

private fun Char.tilBokstav(): Char {
    require(this in 'A'..'Z') { "Ugyldig tegn '$this' i bokstav-tidslinje" }
    return this
}

private fun <T> String.tilTidslinje(
    enhet: Enhet,
    tilVerdi: (Char) -> T,
): Tidslinje<T> {
    val uendeligStart = startsWith("<")
    val uendeligSlutt = endsWith(">")
    val tegn = removePrefix("<").removeSuffix(">")

    require(!(uendeligStart && tegn.startsWith("-"))) { "Uendelig start kan ikke begynne med hull: '$this'" }
    require(!tegn.endsWith("-")) { "Tidslinjen kan ikke slutte med hull: '$this'" }
    require(tegn.isNotEmpty() || (!uendeligStart && !uendeligSlutt)) { "Uendelig tidslinje uten verdier: '$this'" }

    return tegn
        .mapIndexedNotNull { indeks, t ->
            if (t == '-') return@mapIndexedNotNull null
            Periode<T?>(
                verdi = if (t == 'n') null else tilVerdi(t),
                fom = if (uendeligStart && indeks == 0) null else enhet.fom(indeks),
                tom = if (uendeligSlutt && indeks == tegn.lastIndex) null else enhet.tom(indeks),
            )
        }.tilTidslinje() as Tidslinje<T>
}

private fun <T> Tidslinje<T>.somStreng(
    enhet: Enhet,
    tegn: (T & Any) -> Char,
): String {
    if (erTom()) return ""

    val perioder = tilTidslinjePerioderMedDato()
    return buildString {
        perioder.forEachIndexed { indeks, periode ->
            val fom = periode.fom.tilLocalDateEllerNull()
            val tom = periode.tom.tilLocalDateEllerNull()
            val t =
                when (val periodeVerdi = periode.periodeVerdi) {
                    is Verdi -> tegn(periodeVerdi.verdi)
                    is Null -> 'n'
                    is Udefinert -> '-'
                }

            val fraIndeks =
                if (fom == null) {
                    require(indeks == 0) { "Uendelig start midt i tidslinjen" }
                    append('<')
                    0
                } else {
                    enhet.indeksForFom(fom).also { fra ->
                        require(fra >= 0) { "Tidslinjen starter $fom, før $START. Bruk '<' eller flytt START." }
                        if (indeks == 0) append("-".repeat(fra))
                    }
                }

            if (tom == null) {
                append(t).append('>')
            } else {
                val tilIndeks = enhet.indeksForTom(tom)
                require(tilIndeks < MAKS_LENGDE) { "Tidslinjen slutter $tom, for langt frem til å vises som streng" }
                require(tilIndeks >= fraIndeks) { "Perioden slutter $tom, før $START. Bruk '<' eller flytt START." }
                append(t.toString().repeat(tilIndeks - fraIndeks + 1))
            }
        }
    }
}

private fun standardTegn(verdi: Any): Char =
    when (verdi) {
        is Int -> {
            require(verdi in 0..9) { "Kan bare vise tall 0–9 som tegn, fikk $verdi" }
            '0' + verdi
        }

        is Char -> {
            verdi
        }

        is Boolean -> {
            if (verdi) 'J' else 'N'
        }

        else -> {
            error("Kan ikke vise ${verdi::class.simpleName} som tegn. Send med en egen tegn-funksjon.")
        }
    }
