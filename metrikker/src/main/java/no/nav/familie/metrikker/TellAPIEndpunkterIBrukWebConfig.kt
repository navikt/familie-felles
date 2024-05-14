package no.nav.familie.metrikker

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnProperty("familie.tellAPIEndepunkterIBruk.enabled")
open class TellAPIEndpunkterIBrukWebConfig : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(TellAPIEndepunkterIBrukInterceptor()).addPathPatterns("/api/**")
        super.addInterceptors(registry)
    }
}
