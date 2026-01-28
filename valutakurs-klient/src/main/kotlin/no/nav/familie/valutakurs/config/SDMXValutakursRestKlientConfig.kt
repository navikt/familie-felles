package no.nav.familie.valutakurs.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.valutakurs.SDMXRestKlient.Companion.APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
import org.springframework.web.client.RestOperations
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule

@Suppress("SpringFacetCodeInspection")
@Configuration
class SDMXValutakursRestKlientConfig {
    fun xmlMapper(): XmlMapper =
        XmlMapper
            .xmlBuilder()
            .addModule(KotlinModule.Builder().build())
            .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    @Bean("sdmxXmlRestTemplate")
    fun xmlRestTemplate(): RestOperations {
        val converter =
            JacksonXmlHttpMessageConverter(xmlMapper()).apply {
                supportedMediaTypes =
                    listOf(
                        MediaType.parseMediaType(APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA),
                        MediaType.parseMediaType("application/octet-stream"),
                        MediaType.parseMediaType("application/xml;charset=UTF-8"),
                    )
            }
        return RestTemplateBuilder()
            .additionalMessageConverters(converter)
            .build()
    }
}
