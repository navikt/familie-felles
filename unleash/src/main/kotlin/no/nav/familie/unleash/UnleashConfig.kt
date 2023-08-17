package no.nav.familie.unleash

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(UnleashProperties::class)
open class UnleashConfig(
    private val featureToggleProperties: UnleashProperties,
    @Value("\${UNLEASH_SERVER_API_URL}") val apiUrl: String,
    @Value("\${UNLEASH_SERVER_API_TOKEN}") val apiToken: String,
    @Value("\${NAIS_APP_NAME}") val appName: String,
) {

    @Bean
    open fun unleashNext(): UnleashService =
        if (featureToggleProperties.enabled) {
            DefaultUnleashService(apiUrl = apiUrl, apiToken = apiToken, appName = appName)
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "isEnabled gir 'false' med mindre man har oppgitt en annen default verdi.",
            )
            lagDummyUnleashService()
        }

    private fun lagDummyUnleashService(): UnleashService {
        return object : UnleashService {
            override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
                return System.getenv(toggleId).run { toBoolean() } || defaultValue
            }
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(UnleashProperties::class.java)
    }
}

@ConfigurationProperties("unleash")
class UnleashProperties(
    val enabled: Boolean = true,
)

interface UnleashService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
