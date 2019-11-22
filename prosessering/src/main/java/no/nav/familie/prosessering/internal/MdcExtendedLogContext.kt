package no.nav.familie.prosessering.internal

import org.slf4j.MDC
import java.util.regex.Pattern

/**
 * [MDC] backet parameter som tillater en semi-colon separert liste av sub-keys.
 * Kan dermed legge til og fjerne ekstra-kontekst data dynamisk.
 */
class MdcExtendedLogContext private constructor(private val paramName: String) {

    fun add(key: String, value: String) {
        val currentValues = currentValueMap()
        currentValues[key] = value
        MDC.put(paramName, toParamValue(currentValues))
    }

    private fun validateKey(key: String?) {
        require(!(key == null || ILLEGAL_CHARS.matcher(key).find())) { "Ugyldig key: '$key'" }
    }

    fun getValue(key: String): String? {
        validateKey(key)
        val currentValues = currentValueMap()
        return currentValues[key]
    }

    fun remove(key: String) {
        validateKey(key)
        val currentValues = currentValueMap()
        currentValues.remove(key)
        MDC.put(paramName, toParamValue(currentValues))
    }

    private fun currentValueMap(): MutableMap<String, String> {
        val currentValues = MDC.get(paramName) ?: return mutableMapOf()
        return currentValues
                .substring(paramName.length + 1, currentValues.length - 1)
                .split(";")
                .associateBy({ it.substringBefore('=') }, { it.substringAfter('=') })
                .toMutableMap()
    }

    private fun toParamValue(elements: Map<String, String>): String {
        return paramName + "[" + elements.map { it.key + '=' + it.value }.joinToString(";") + "]"
    }

    fun clear() {
        MDC.remove(paramName)
    }

    companion object {

        private val ILLEGAL_CHARS = Pattern.compile("[\\[\\];=]")

        fun getContext(kontekstParamNavn: String): MdcExtendedLogContext {
            return MdcExtendedLogContext(kontekstParamNavn)
        }
    }
}
