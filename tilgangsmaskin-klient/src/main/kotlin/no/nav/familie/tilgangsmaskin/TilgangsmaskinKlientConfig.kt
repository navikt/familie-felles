package no.nav.familie.tilgangsmaskin

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.net.URI

@Configuration
class TilgangsmaskinKlientConfig {
    @Bean
    fun tilgangsmaskinKlient(
        @Value("\${TILGANGSMASKIN_API_URL}") tilgangsmaskinUri: URI,
        @Qualifier(TILGANGSMASKIN_REST_CLIENT) restClient: RestClient,
    ): TilgangsmaskinKlient = TilgangsmaskinKlient(tilgangsmaskinUri, restClient)

    companion object {
        const val TILGANGSMASKIN_REST_CLIENT = "tilgangsmaskinRestClient"
    }
}
