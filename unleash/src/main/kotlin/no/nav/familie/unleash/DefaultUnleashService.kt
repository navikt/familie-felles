package no.nav.familie.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.strategy.Strategy
import io.getunleash.util.UnleashConfig

class DefaultUnleashService(
    private val apiUrl: String,
    private val apiToken: String,
    private val appName: String,
    private val strategies: List<Strategy>
) : UnleashService {

    private val defaultUnleash: DefaultUnleash

    init {
        defaultUnleash = DefaultUnleash(
            UnleashConfig.builder()
                .appName(appName)
                .unleashAPI("$apiUrl/api")
                .apiKey(apiToken)
                .unleashContextProvider(lagUnleashContextProvider())
                .build(),
            *strategies.toTypedArray()
        )
    }

    private fun lagUnleashContextProvider(): UnleashContextProvider {
        return UnleashContextProvider {
            UnleashContext.builder()
                .appName(appName)
                .build()
        }
    }

    override fun isEnabled(toggleId: String, defaultValue: Boolean): Boolean {
        return defaultUnleash.isEnabled(toggleId, defaultValue)
    }

    override fun destroy() {
        // Spring trigger denne ved shutdown. Gjøres for å unngå at unleash fortsetter å gjøre kall ut
        defaultUnleash.shutdown()
    }
}
