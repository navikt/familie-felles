package no.nav.familie.tidslinje

const val INF = 1_000_000_000L

sealed class PeriodeVerdi<T>(
    protected val _verdi: T?,
) {
    override operator fun equals(other: Any?): Boolean {
        if (other !is PeriodeVerdi<*>) return false
        if (other._verdi == this._verdi) return true
        return false
    }

    override fun hashCode(): Int = this._verdi.hashCode()

    abstract val verdi: T?
}

class Verdi<T>(
    override val verdi: T & Any,
) : PeriodeVerdi<T>(verdi)

class Udefinert<T> : PeriodeVerdi<T>(null) {
    override fun equals(other: Any?): Boolean = other is Udefinert<*>

    override fun hashCode(): Int = this._verdi.hashCode()

    override val verdi: T? = this._verdi
}

class Null<T> : PeriodeVerdi<T>(null) {
    override fun equals(other: Any?): Boolean = other is Null<*>

    override fun hashCode(): Int = this._verdi.hashCode()

    override val verdi: T? = this._verdi
}

/**
 * En periode representerer et tidsintervall hvor en tidslinje har en konstant verdi.
 * En periode varer en tid [lengde], og kan være uendelig.
 * Om [lengde] > [INF] eller [erUendelig] er satt til true, behandles perioden som at den har uendelig lengde.
 * En tidslinje støtter verdier av typen [Udefinert], [Null] og [PeriodeVerdi]. En verdi er udefinert når vi ikke vet
 * hva verdien skal være (et hull i tidslinja). En verdi er no.nav.familie.tidslinje.Null når vi vet at det ikke finnes en verdi i dette tidsrommet.
 */
data class TidslinjePeriode<T>(
    val periodeVerdi: PeriodeVerdi<T>,
    var lengde: Long,
    var erUendelig: Boolean = false,
) {
    init {
        if (lengde >= INF) {
            erUendelig = true
        }
        if (erUendelig && lengde < INF) {
            lengde = INF
        }
        if (lengde <= 0) {
            throw java.lang.IllegalArgumentException("lengde må være større enn null.")
        }
    }

    constructor(periodeVerdi: T?, lengde: Long, erUendelig: Boolean = false) : this(
        if (periodeVerdi == null) {
            Null()
        } else {
            Verdi(
                periodeVerdi,
            )
        },
        lengde,
        erUendelig,
    )

    override fun toString(): String = "Verdi: " + periodeVerdi.verdi.toString() + ", Lengde: " + lengde
}

fun <T> T?.tilPeriodeVerdi(): PeriodeVerdi<T> = this?.let { Verdi(it) } ?: Null()
