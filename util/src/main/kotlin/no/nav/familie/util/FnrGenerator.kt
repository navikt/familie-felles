package no.nav.familie.util

/**
 * Fødselsnummergenerator. Genererer tilfeldige fødselsnumre med mulighet for å spesifisere år, måned, dato og D-nummer.
 */
object FnrGenerator {

    private val k1Vekting = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
    private val k2Vekting = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)


    fun generer(år: Int = (1854..2039).random(),
                måned: Int = (1..12).random(),
                dag: Int = (1..28).random(),
                erDnummer: Boolean = false): String {

        if (år > 2039 || år < 1854) {
            error("Ugyldig årstall. Lovlige verdier er mellom 1854 og 2039")
        }

        val datoString = formater(år, måned, dag, erDnummer)

        while (true) {
            val fødselsnummer = lagFødselsnummer(år, datoString)
            if (fødselsnummer.length == 11) {
                return fødselsnummer
            }
        }
    }

    private fun lagFødselsnummer(år: Int, datoString: String): String {
        val personnummerUtenSjekksiffer = when (år / 100) {
            19 -> (0..499).random().toString().padStart(3, '0')
            18 -> (500..749).random().toString().padStart(3, '0')
            20 -> (500..999).random().toString().padStart(3, '0')
            else -> error("Ugylidg århundre")
        }
        val verdi = datoString + personnummerUtenSjekksiffer

        val siffer = verdi.chunked(1).map { it.toInt() }

        val kontrollMod1 = 11 - (0..8).sumBy { k1Vekting[it] * siffer[it] } % 11
        val kontrollsiffer1 = kontrollSiffer(kontrollMod1)

        val sifferMedEttKontrollsiffer = siffer + kontrollsiffer1
        val kontrollMod2 = 11 - (0..9).sumBy { k2Vekting[it] * sifferMedEttKontrollsiffer[it] } % 11
        val kontrollsiffer2 = kontrollSiffer(kontrollMod2)

        return verdi + kontrollsiffer1 + kontrollsiffer2
    }

    private fun formater(år: Int, måned: Int, dag: Int, erDnummer: Boolean): String {
        val dagString = if (erDnummer) (dag + 40).toString() else dag.toString().padStart(2, '0')
        val månedString = måned.toString().padStart(2, '0')
        val årString = (år % 100).toString().padStart(2, '0')
        return dagString + månedString + årString
    }

    private fun kontrollSiffer(kontrollMod: Int): Int {
        if (kontrollMod == 11) {
            return 0
        }
        return kontrollMod
    }
}
