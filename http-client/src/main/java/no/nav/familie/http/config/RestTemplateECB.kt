package no.nav.familie.http.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.http.interceptor.ECBRestClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations

@Configuration
@Import(ECBRestClientInterceptor::class)
class RestTemplateECB {

    @Bean
    fun xmlMapper(): XmlMapper {
        val mapper = XmlMapper()
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        mapper.registerKotlinModule()
        return mapper
    }

    @Bean("ecb")
    fun xmlRestTemplate(ecbRestClientInterceptor: ECBRestClientInterceptor): RestOperations {
        val converter = MappingJackson2HttpMessageConverter(xmlMapper())
        converter.supportedMediaTypes = listOf(MediaType.parseMediaType("application/vnd.sdmx.genericdata+xml;version=2.1"))
        return RestTemplateBuilder()
            .additionalMessageConverters(converter)
            .additionalInterceptors(ecbRestClientInterceptor)
            .build()
    }
}
