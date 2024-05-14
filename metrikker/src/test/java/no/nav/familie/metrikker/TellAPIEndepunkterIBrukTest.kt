package no.nav.familie.metrikker

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

class TellAPIEndepunkterIBrukTest {
    val applicationContext: ApplicationContext = mockk()
    val requestMappingHandlerMapping: RequestMappingHandlerMapping = mockk()
    val info: RequestMappingInfo = mockk()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `skal opprette map med key og counter når det finnes en requestmapping`() {
        settOppTestData("/api/foo", RequestMethod.GET)

        TellAPIEndepunkterIBrukInitialiserer(
            "test",
            applicationContext,
            listOf("/internal"),
        ).populerMapMedCountersForRestEndepunkt()

        assertThat(TellAPIEndepunkterIBrukInitialiserer.metrikkerForEndepunkter).hasSize(1)
        assertThat(TellAPIEndepunkterIBrukInitialiserer.metrikkerForEndepunkter.containsKey("[GET]/api/foo")).isTrue()
        assertThat(
            TellAPIEndepunkterIBrukInitialiserer.metrikkerForEndepunkter.get("[GET]/api/foo")?.id?.name,
        ).isEqualTo("test.GET.api.foo")
    }

    @Test
    fun `skal opprette map med key og counter når det finnes en requestmapping med pathParam og navnet på counteren saneres`() {
        settOppTestData("/api/foo/{fooId}", RequestMethod.POST)

        TellAPIEndepunkterIBrukInitialiserer(
            "test",
            applicationContext,
            listOf("/internal"),
        ).populerMapMedCountersForRestEndepunkt()

        assertThat(TellAPIEndepunkterIBrukInitialiserer.metrikkerForEndepunkter).hasSize(1)
        assertThat(TellAPIEndepunkterIBrukInitialiserer.metrikkerForEndepunkter.containsKey("[POST]/api/foo/{fooId}")).isTrue()
        assertThat(
            TellAPIEndepunkterIBrukInitialiserer.metrikkerForEndepunkter.get("[POST]/api/foo/{fooId}")?.id?.name,
        ).isEqualTo("test.POST.api.foo.fooId")
    }

    @Test
    fun `skal ikke opprette map med counter for annet enn pathparam som starter med api`() {
        settOppTestData("/internal/foobar", RequestMethod.POST)

        TellAPIEndepunkterIBrukInitialiserer(
            "test",
            applicationContext,
            listOf("/internal"),
        ).populerMapMedCountersForRestEndepunkt()

        assertThat(TellAPIEndepunkterIBrukInitialiserer.metrikkerForEndepunkter).hasSize(0)
    }

    private fun settOppTestData(
        path: String,
        method: RequestMethod,
    ) {
        every { info.patternValues } returns setOf(path)
        every { info.methodsCondition } returns RequestMethodsRequestCondition(method)

        every { requestMappingHandlerMapping.handlerMethods } returns
            mapOf(Pair(info, mockk()))
        every {
            applicationContext.getBean(
                "requestMappingHandlerMapping",
                RequestMappingHandlerMapping::class.java,
            )
        } returns
            requestMappingHandlerMapping
    }
}
