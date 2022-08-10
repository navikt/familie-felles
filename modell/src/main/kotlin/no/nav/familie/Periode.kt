package no.nav.familie

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.Temporal

sealed class Periode<T>(
    open val fom: T,
    open val tom: T,
) : Comparable<Periode<T>> where T : Comparable<T>, T : Temporal {

    protected fun validate() {
        require(tom >= fom) { "Til-og-med før fra-og-med: $fom > $tom" }
    }

    infix fun inneholder(dato: T): Boolean {
        return dato in fom..tom
    }

    infix fun inneholder(annen: Periode<T>): Boolean {
        return annen.fom >= this.fom && annen.tom <= this.tom
    }

    infix fun omsluttesAv(annen: Periode<T>): Boolean {
        return annen.fom <= fom && annen.tom >= tom
    }

    infix fun overlapper(other: Periode<T>): Boolean {
        return inneholder(other.fom) || inneholder(other.tom) || other.inneholder(fom)
    }

    open infix fun snitt(annen: Periode<T>): Periode<T>? {
        return if (!overlapper(annen)) {
            null
        } else if (this == annen) {
            this
        } else {
            lagPeriode(
                maxOf(fom, annen.fom),
                minOf(tom, annen.tom)
            )
        }
    }

    open infix fun union(annen: Periode<T>): Periode<T> {
        return if (overlapper(annen) || this.påfølgesAv(annen) || annen.påfølgesAv(this)) {
            lagPeriode(
                minOf(fom, annen.fom),
                maxOf(tom, annen.tom)
            )
        } else {
            error("Kan ikke lage union av perioder som $this og $annen som ikke overlapper eller direkte følger hverandre.")
        }
    }

    infix fun overlapperKunIStartenAv(annen: Periode<T>) =
        annen.fom in fom..tom && tom < annen.tom

    infix fun overlapperKunISluttenAv(annen: Periode<T>) =
        annen.tom in fom..tom && fom > annen.fom

    abstract infix fun påfølgesAv(påfølgende: Periode<T>): Boolean

    abstract fun lengdeIHeleMåneder(): Long

    override fun compareTo(other: Periode<T>): Int {
        return Comparator.comparing(Periode<T>::fom).thenComparing(Periode<T>::tom).compare(this, other)
    }

    abstract fun lagPeriode(fom: T, tom: T): Periode<T>
}

data class Månedsperiode(override val fom: YearMonth, override val tom: YearMonth) : Periode<YearMonth>(fom, tom) {

    init {
        validate()
    }

    val fomDato get() = fom.atDay(1)
    val tomDato get() = tom.atEndOfMonth()

    constructor(fom: LocalDate, tom: LocalDate) : this(YearMonth.from(fom), YearMonth.from(tom))

    override fun lagPeriode(fom: YearMonth, tom: YearMonth): Månedsperiode {
        return Månedsperiode(fom, tom)
    }

    override infix fun union(annen: Periode<YearMonth>): Månedsperiode {
        return super.union(annen) as Månedsperiode
    }

    override infix fun snitt(annen: Periode<YearMonth>): Månedsperiode? {
        return super.snitt(annen) as Månedsperiode?
    }

    override infix fun påfølgesAv(påfølgende: Periode<YearMonth>): Boolean {
        return this.tom.plusMonths(1) == påfølgende.fom
    }

    override fun lengdeIHeleMåneder(): Long {
        return (tom.year * 12 + tom.monthValue) - (fom.year * 12 + fom.monthValue) + 1L
    }

    fun toDatoperiode() = Datoperiode(fomDato, tomDato)
}

data class Datoperiode(override val fom: LocalDate, override val tom: LocalDate) : Periode<LocalDate>(fom, tom) {

    init {
        validate()
    }

    val fomMåned get() = YearMonth.from(fom)
    val tomMåned get() = YearMonth.from(tom)

    constructor(fom: YearMonth, tom: YearMonth) : this(fom.atDay(1), tom.atEndOfMonth())

    override fun lagPeriode(fom: LocalDate, tom: LocalDate): Datoperiode {
        return Datoperiode(fom, tom)
    }

    override infix fun union(annen: Periode<LocalDate>): Datoperiode {
        return super.union(annen) as Datoperiode
    }

    override infix fun snitt(annen: Periode<LocalDate>): Datoperiode? {
        return super.snitt(annen) as Datoperiode?
    }

    override infix fun påfølgesAv(påfølgende: Periode<LocalDate>): Boolean {
        return this.tom.plusDays(1) == påfølgende.fom
    }

    override fun lengdeIHeleMåneder(): Long {
        require(fom.dayOfMonth == 1 && tom == YearMonth.from(tom).atEndOfMonth()) {
            "Forsøk på å beregne lengde i hele måneder for en periode som ikke er hele måneder: $fom - $tom"
        }
        return (tom.year * 12 + tom.monthValue) - (fom.year * 12 + fom.monthValue) + 1L
    }

    fun toMånedsperiode() = Månedsperiode(fomMåned, tomMåned)
}

fun <T> List<Periode<T>>.erSammenhengende(): Boolean where T : Comparable<T>, T : Temporal =
    this.sorted().foldIndexed(true) { index, acc, periode ->
        if (index == 0) {
            acc
        } else {
            val forrigePeriode = this[index - 1]
            when {
                forrigePeriode.påfølgesAv(periode) -> acc
                else -> false
            }
        }
    }

fun <T> List<Periode<T>>.harOverlappende(): Boolean where T : Comparable<T>, T : Temporal =
    this.sorted().zipWithNext { a, b ->
        a.overlapper(b)
    }.any { it }
