package no.nav.familie.prosessering.internal

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.slf4j.MDC

class MdcExtendedLogContextTest {

    private val paramName = "Bob"

    private var mdcExtendedLogContext: MdcExtendedLogContext = MdcExtendedLogContext.getContext(paramName)

    @Before
    fun setup() {
        mdcExtendedLogContext.clear()
    }

    @Test
    fun `add legger til verdi`() {
        mdcExtendedLogContext.add("Blåbær", "5 kr")

        assertThat(MDC.get(paramName)).isEqualTo("$paramName[Blåbær=5 kr]")
    }

    @Test
    fun `getValue henter ut en verdi`() {
        mdcExtendedLogContext.add("Blåbær", "5 kr")

        val value = mdcExtendedLogContext.getValue("Blåbær")

        assertThat(value).isEqualTo("5 kr")
    }

    @Test
    fun `remove fjerner en verdi`() {
        mdcExtendedLogContext.add("Blåbær", "5 kr")
        mdcExtendedLogContext.add("Bringebær", "15 kr")

        mdcExtendedLogContext.remove("Blåbær")

        assertThat(MDC.get(paramName)).isEqualTo("$paramName[Bringebær=15 kr]")
    }

    @Test
    fun `add kaster exception ved ugyldig nøkkel`() {
        val key = "Bl[bær"

        assertThatThrownBy { mdcExtendedLogContext.getValue(key) }
                .isEqualToComparingFieldByField(IllegalArgumentException("Ugyldig key: '$key'"))
    }

    @Test
    fun `getValue kaster exception ved ugyldig nøkkel`() {
        val key = "Bl]bær"

        assertThatThrownBy { mdcExtendedLogContext.getValue(key) }
                .isEqualToComparingFieldByField(IllegalArgumentException("Ugyldig key: '$key'"))
    }

    @Test
    fun `remove kaster exception ved ugyldig nøkkel`() {
        val key = "Bl=bær"

        assertThatThrownBy { mdcExtendedLogContext.getValue(key) }
                .isEqualToComparingFieldByField(IllegalArgumentException("Ugyldig key: '$key'"))
    }

}
