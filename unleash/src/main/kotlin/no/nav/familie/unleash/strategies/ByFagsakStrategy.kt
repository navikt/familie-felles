package no.nav.familie.unleash.strategies

import io.getunleash.strategy.Strategy
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class ByFagsakStrategy : Strategy {

    override fun getName(): String {
        return "byFagsakId"
    }

    override fun isEnabled(properties: MutableMap<String, String>): Boolean {
        return isEnabledForFagsakId(properties)
    }

    private fun isEnabledForFagsakId(properties: MutableMap<String, String>): Boolean {
        val fagsakId = MDC.get(MDC_FAKSAK_ID_KEY)
        return properties[MDC_FAKSAK_ID_KEY]
            ?.split(',')
            ?.any { tillatteFagsakIder -> fagsakId == tillatteFagsakIder }
            ?: false
    }

    companion object {
        val MDC_FAKSAK_ID_KEY: String = "fagsakId"
    }
}

fun <T, R> withFagsakId(fagsakId: T, fn: () -> R): R {
    MDC.put(ByFagsakStrategy.MDC_FAKSAK_ID_KEY, fagsakId.toString())
    val res = fn()
    MDC.remove(ByFagsakStrategy.MDC_FAKSAK_ID_KEY)
    return res
}
