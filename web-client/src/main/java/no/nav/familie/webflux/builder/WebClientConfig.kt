package no.nav.familie.webflux.builder

import no.nav.familie.webflux.filter.ConsumerIdFilter
import no.nav.familie.webflux.filter.MdcValuesPropagatingFilter
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientFilterFunction
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientCodecCustomizer
import org.springframework.boot.web.codec.CodecCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.core.annotation.Order
import org.springframework.http.client.reactive.JettyClientHttpConnector
import org.springframework.http.client.reactive.JettyResourceFactory
import org.springframework.util.ClassUtils
import org.springframework.web.reactive.function.client.WebClient
import java.util.stream.Collectors

const val FAMILIE_WEB_CLIENT_BUILDER = "familieWebClientBuilder"

/**
 * Då vi bruker jetty som server så brukes jetty i stedet for netty her for å unngå konflikter
 *
 * https://github.com/jetty-project/jetty-reactive-httpclient
 *
 * Hvis man ønsker å bruke WebClientBuilder generellt men ikke få med metrics, ekskludere [WebClientMetricsConfiguration]
 */
@Configuration
@Import(ConsumerIdFilter::class, WebClientAutoConfiguration::class)
class WebClientConfig {

    /**
     * Overskrever [WebClientCodecsConfiguration] fordi den ikke blir initiert av noen grunn
     * [org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration.WebClientCodecsConfiguration]
     *
     * Med denne blir objectMapper-bean plukket opp og brukt for encoding/decoding
     */
    @Bean
    @ConditionalOnProperty("familie.web.exchangeStrategiesCustomizer", matchIfMissing = true)
    fun exchangeStrategiesCustomizer(codecCustomizers: ObjectProvider<CodecCustomizer>): WebClientCodecCustomizer {
        return WebClientCodecCustomizer(codecCustomizers.orderedStream().collect(Collectors.toList()))
    }

    /**
     * Spring har default 256KB, denne settes automatisk til unlimited for å unngå trøbbel.
     * Deaktiveres hvis man setter `spring.codec.max-in-memory-size`, til eks `100MB`
     * Default: [org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration.DefaultCodecsConfiguration]
     */
    @Bean
    @Order(-1)
    @ConditionalOnProperty("spring.codec.max-in-memory-size", matchIfMissing = true, havingValue = "Umulig verdi")
    fun codecCustomizer(): CodecCustomizer {
        return CodecCustomizer { it.defaultCodecs().maxInMemorySize(-1) }
    }

    /**
     * Scope: prototype - for å generere en ny webbuilder for hver gang som den blir injectad, ellers er den singleton
     *
     * @param connectTimeout – the max time, in milliseconds, a connection can take to connect to destinations. Zero value means infinite timeout.
     * @param socketTimeout - the socket address resolution timeout
     * @param requestTimeout - the max time, in milliseconds, a connection can be idle (that is, without traffic of bytes in either direction)
     * @param webClientMetricsEnabled Pga av at MetricsWebClientCustomizer legger inn metric for hver url, og vi ikke bruker URI templates
     *                       så fjernes MetricsWebClientCustomizer for å unngå allt for mye metrics. Kan tas i bruk med property. Hvis den hadde brukt Order hadde det vært enklere å skru den av
     */
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Bean(FAMILIE_WEB_CLIENT_BUILDER)
    fun webClientBuilder(
        webClientBuilder: WebClient.Builder,
        jettyResourceFactory: JettyResourceFactory,
        consumerIdFilter: ConsumerIdFilter,
        @Value("\${familie.web.timeout.connect:2000}") connectTimeout: Long,
        @Value("\${familie.web.timeout.socket:15000}") socketTimeout: Long,
        @Value("\${familie.web.timeout.requestTimeout:30000}") requestTimeout: Long,
        @Value("\${familie.web.web-metrics.enabled:false}") webClientMetricsEnabled: Boolean
    ): WebClient.Builder {
        val httpClient = lagHttpClient(connectTimeout, socketTimeout, requestTimeout)

        if (!webClientMetricsEnabled) {
            webClientBuilder.filters { filters -> filters.removeIf { it is MetricsWebClientFilterFunction } }
        }

        return webClientBuilder
            .filter(consumerIdFilter)
            .filter(MdcValuesPropagatingFilter())
            .clientConnector(JettyClientHttpConnector(httpClient, jettyResourceFactory))
    }

    /**
     * Då vi fortsatt bruker jetty server brukes jetty-klienten for å unngå konflikt med netty.
     * Sjekker her at jetty client finnes, då det kun er satt opp timeout-config for jetty og ikke de andre klientene
     */
    private fun lagHttpClient(
        connectTimeout: Long,
        socketTimeout: Long,
        requestTimeout: Long
    ): HttpClient {
        if (!ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", this::class.java.classLoader)) {
            error("Har ikke implementert støtte for andre clienter enn reactor client")
        }

        return HttpClient(SslContextFactory.Client()).apply {
            this.connectTimeout = connectTimeout
            this.addressResolutionTimeout = socketTimeout
            this.idleTimeout = requestTimeout
        }
    }
}
