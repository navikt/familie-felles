package no.nav.familie.http.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestTemplateBuilderBean {
    /**
     * Denne bønnnen initialiseres hvis proxy-url ikke finnes. Hvis proxy-url finnnes vil bønnen over initialiseres og
     * denne det ikke med mindre proxyen har verdien "umulig verdi", som den aldri skal ha.
     */
    @Bean
    fun restTemplateBuilderNoProxy(): RestClient.Builder {
        return RestClient.builder()
    }
}
