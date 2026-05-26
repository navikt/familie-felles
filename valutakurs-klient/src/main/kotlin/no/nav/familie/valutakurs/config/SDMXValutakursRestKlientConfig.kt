package no.nav.familie.valutakurs.config

import no.nav.familie.valutakurs.SDMXRestKlient.Companion.APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter
import org.springframework.web.client.RestClient
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

    @Bean("sdmxXmlRestClient")
    fun xmlRestClient(): RestClient {
        val converter =
            JacksonXmlHttpMessageConverter(xmlMapper()).apply {
                supportedMediaTypes =
                    listOf(
                        MediaType.parseMediaType(APPLICATION_CONTEXT_SDMX_XML_2_1_GENERIC_DATA),
                        MediaType.parseMediaType("application/octet-stream"),
                        MediaType.parseMediaType("application/xml;charset=UTF-8"),
                    )
            }
        return RestClient
            .builder()
            .requestFactory(SimpleClientHttpRequestFactory())
            .messageConverters { converters ->
                converters.add(0, converter)
            }.build()
    }
}
