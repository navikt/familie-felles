package no.nav.familie.valutakurs.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.valutakurs.ValutakursRestClient.Companion.APPLICATION_CONTEXT_SDMX_ML_2_1_GENERIC_DATA
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations

@Suppress("SpringFacetCodeInspection")
@Configuration
class ValutakursRestClientConfig {
    fun xmlMapper(): XmlMapper {
        val mapper =
            XmlMapper().apply {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                registerKotlinModule()
            }
        return mapper
    }

    @Bean("ecbRestTemplate")
    fun xmlRestTemplate(): RestOperations {
        val converter =
            MappingJackson2HttpMessageConverter(xmlMapper()).apply {
                supportedMediaTypes =
                    listOf(
                        MediaType.parseMediaType(APPLICATION_CONTEXT_SDMX_ML_2_1_GENERIC_DATA),
                        MediaType.parseMediaType("application/octet-stream"),
                    )
            }
        return RestTemplateBuilder()
            .additionalMessageConverters(converter)
            .build()
    }
}
