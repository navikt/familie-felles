package no.nav.familie.webflux.builder

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.actuate.metrics.AutoTimer
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebClientMetricConfig {

    @Bean
    fun metricsWebClientCustomizer() = MetricsWebClientCustomizer(
        SimpleMeterRegistry(),
        { _, _, _ -> emptyList<Tag>() },
        "",
        AutoTimer.ENABLED
    )
}
