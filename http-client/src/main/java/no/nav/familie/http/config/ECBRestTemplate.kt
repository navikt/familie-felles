package no.nav.familie.http.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.familie.http.interceptor.ECBRestClientInterceptor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestOperations

@Configuration
@Import(ECBRestClientInterceptor::class)
class ECBRestTemplate {

    @Bean("ecbMapper")
    fun xmlMapper(): XmlMapper {
        val mapper = XmlMapper().apply {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerKotlinModule()
        }
        return mapper
    }

    @Bean("ecbRestTemplate")
    fun xmlRestTemplate(ecbRestClientInterceptor: ECBRestClientInterceptor, @Qualifier("ecbMapper") xmlMapper: XmlMapper): RestOperations {
        val converter = MappingJackson2HttpMessageConverter(xmlMapper).apply {
            supportedMediaTypes = listOf(MediaType.parseMediaType("application/vnd.sdmx.genericdata+xml;version=2.1"))
        }
        return RestTemplateBuilder()
            .additionalMessageConverters(converter)
            .additionalInterceptors(ecbRestClientInterceptor)
            .build()
    }
}
