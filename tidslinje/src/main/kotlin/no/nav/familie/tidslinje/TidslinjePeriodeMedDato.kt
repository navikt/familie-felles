package no.nav.familie.tidslinje

import java.time.LocalDate

data class TidslinjePeriodeMedDato<T>(
    val periodeVerdi: PeriodeVerdi<T>,
    val fom: Dato,
    val tom: Dato,
) {
    constructor(
        verdi: T?,
        fom: LocalDate?,
        tom: LocalDate?,
    ) : this(
        periodeVerdi = verdi?.let { Verdi(it) } ?: Null(),
        fom = Dato(fom ?: PRAKTISK_TIDLIGSTE_DAG),
        tom = Dato(tom ?: PRAKTISK_SENESTE_DAG),
    )

    class Dato(
        private val dato: LocalDate,
    ) : Comparable<Dato> {
        fun tilLocalDateEllerNull(): LocalDate? =
            if (this.dato == PRAKTISK_TIDLIGSTE_DAG || this.dato == PRAKTISK_SENESTE_DAG) {
                null
            } else {
                this.dato
            }

        override fun compareTo(other: Dato): Int = this.dato.compareTo(other.dato)
    }

    fun tilPeriode() = Periode(periodeVerdi.verdi, fom.tilLocalDateEllerNull(), tom.tilLocalDateEllerNull())
}

fun <T> List<TidslinjePeriodeMedDato<T>>.tilTidslinje(): Tidslinje<T> {
    val perioder = this.tilTidslinjePerioder()
    return Tidslinje(
        startsTidspunkt = this.firstOrNull()?.fom?.tilDatoEllerPraktiskTidligsteDag() ?: PRAKTISK_TIDLIGSTE_DAG,
        perioder = perioder,
    )
}

private fun <T> List<TidslinjePeriodeMedDato<T>>.fyllInnTommePerioder(): List<TidslinjePeriodeMedDato<T>> =
    this.fold(emptyList()) { periodeListeMedTommePerioder, periode ->
        val sisteElement = periodeListeMedTommePerioder.lastOrNull()

        if (sisteElement == null) {
            periodeListeMedTommePerioder + periode
        } else if (sisteElement.tom.tilDatoEllerPraktiskSenesteDag() ==
            periode.fom
                .tilDatoEllerPraktiskTidligsteDag()
                .minusDays(1)
        ) {
            periodeListeMedTommePerioder + periode
        } else {
            periodeListeMedTommePerioder +
                    TidslinjePeriodeMedDato(
                        Udefinert(),
                        TidslinjePeriodeMedDato.Dato(sisteElement.tom.tilDatoEllerPraktiskSenesteDag().plusDays(1)),
                        TidslinjePeriodeMedDato.Dato(periode.fom.tilDatoEllerPraktiskTidligsteDag().minusDays(1)),
                    ) +
                    periode
        }
    }

private fun <T> List<TidslinjePeriodeMedDato<T>>.tilTidslinjePerioder(): List<TidslinjePeriode<T>> {
    this.validerKunFørsteEllerSistePeriodeErUendelig()
    this.validerIngenOverlapp()

    return this
        .sortedBy { it.fom }
        .fyllInnTommePerioder()
        .map {
            TidslinjePeriode(
                periodeVerdi = it.periodeVerdi,
                lengde = it.fom.tilDatoEllerPraktiskTidligsteDag().diffIDager(it.tom.tilDatoEllerPraktiskSenesteDag()),
                erUendelig = it.tom.tilLocalDateEllerNull() == null,
            )
        }
}

fun <T> List<TidslinjePeriodeMedDato<T>>.validerIngenOverlapp(feilmelding: String = "Feil med tidslinje. Overlapp på periode") {
    this
        .sortedBy { it.fom }
        .zipWithNext { a, b ->
            if (a.tom.tilDatoEllerPraktiskSenesteDag().isAfter(b.fom.tilDatoEllerPraktiskTidligsteDag())) {
                error(message = feilmelding)
            }
        }
}

private fun <T> List<TidslinjePeriodeMedDato<T>>.validerKunFørsteEllerSistePeriodeErUendelig() {
    val sortertListe = this.sortedBy { it.fom }

    sortertListe.forEachIndexed { indeks, periode ->
        if (indeks != 0 && periode.fom.tilLocalDateEllerNull() == null) {
            error("Feil med tidslinje. Flere perioder med fom=null")
        }
        if (indeks != sortertListe.lastIndex && periode.tom.tilLocalDateEllerNull() == null) {
            error("Feil med tidslinje. Periode som ikke kommer på slutten har tom=null")
        }
    }
}

private fun TidslinjePeriodeMedDato.Dato.tilDatoEllerPraktiskTidligsteDag(): LocalDate =
    this.tilLocalDateEllerNull() ?: PRAKTISK_TIDLIGSTE_DAG

private fun TidslinjePeriodeMedDato.Dato.tilDatoEllerPraktiskSenesteDag(): LocalDate =
    this.tilLocalDateEllerNull() ?: PRAKTISK_SENESTE_DAG
