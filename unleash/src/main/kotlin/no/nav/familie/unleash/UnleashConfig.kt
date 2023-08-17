package no.nav.familie.unleash

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@EnableConfigurationProperties(UnleashProperties::class)
@Component
class UnleashConfig(
    private val featureToggleProperties: UnleashProperties,
    @Value("\${UNLEASH_SERVER_API_URL}") val apiUrl: String,
    @Value("\${UNLEASH_SERVER_API_TOKEN}") val apiToken: String,
    @Value("\${NAIS_APP_NAME}") val appName: String
) {

    @Bean("unleashNext")
    fun unleashNext(): UnleashService =
        if (featureToggleProperties.enabled) {
            UnleashNextFeatureToggleService(apiUrl = apiUrl, apiToken = apiToken, appName = appName)
        } else {
            logger.warn(
                "Funksjonsbryter-funksjonalitet er skrudd AV. " +
                    "Gir standardoppførsel for alle funksjonsbrytere, dvs 'false'"
            )
            lagDummyFeatureToggleService()
        }

    private fun lagDummyFeatureToggleService(): UnleashService {
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
    val enabled: Boolean = true
)

interface UnleashService {

    fun isEnabled(toggleId: String): Boolean {
        return isEnabled(toggleId, false)
    }

    fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean
}
