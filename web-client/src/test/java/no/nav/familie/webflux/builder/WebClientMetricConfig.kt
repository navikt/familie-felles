package no.nav.familie.webflux.builder

import io.mockk.mockk
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebClientMetricConfig {

    @Bean
    fun metricsWebClientCustomizer() = MetricsWebClientCustomizer(mockk(), mockk(), "", mockk())
}
