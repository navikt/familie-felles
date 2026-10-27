package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * zipMedNeste gir hver periode et par av verdien i forrige og denne perioden. Paret vises som to sifre,
 * der `n` betyr null, og hver rad sammenligner periodene og plasseringen.
 */
class ZipMedNesteTest {
    @ParameterizedTest(name = "{0} med {1}: {2} på {3}")
    @CsvSource(
        delimiter = '|',
        value = [
            // Uten padding: hvert par havner på perioden til den siste verdien
            "123     | INGEN_PADDING   | 12,23        | -xy",
            // Padding før: første periode får null som forrige verdi
            "123     | FØR             | n1,12,23     | xyz",
            // Padding etter: hvert par havner på perioden til den første verdien
            "123     | ETTER           | 12,23,3n     | xyz",
            // Null og hull gir null i paret
            "1n2     | INGEN_PADDING   | 1n,n2        | -xy",
            "1-2     | INGEN_PADDING   | 1n,n2        | -xy",
            "12>     | INGEN_PADDING   | 12           | -x>",
            "1       | INGEN_PADDING   | ''           | ''",
        ],
    )
    fun `zipMedNeste kobler hver periode med perioden før`(
        tidslinje: String,
        padding: ZipPadding,
        par: String,
        plassering: String,
    ) {
        val resultat = tidslinje.mnd().zipMedNeste(padding)
        val parSomTekst = resultat.tilPerioder().map { "${it.verdi!!.first ?: "n"}${it.verdi!!.second ?: "n"}" }

        assertEquals(par.split(",").filter { it.isNotEmpty() }, parSomTekst)
        assertEquals(plassering, resultat.somMnd { 'x' + parSomTekst.indexOf("${it.first ?: "n"}${it.second ?: "n"}") })
    }
}
