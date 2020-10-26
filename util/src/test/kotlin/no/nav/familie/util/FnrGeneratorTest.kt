package no.nav.familie.util

import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import org.junit.jupiter.api.Test


class FnrGeneratorTest {

    @Test
    fun `generer genererer kun gyldige fødselsnumre`() {

        repeat(10000) {
            Fødselsnummer(FnrGenerator.generer())
        }
    }
}
