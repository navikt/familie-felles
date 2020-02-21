package no.nav.familie.log

enum class NavHttpHeaders(private val header: String) {
    NAV_PERSONIDENT("Nav-Personident"), NAV_CALL_ID("Nav-Call-Id"), NAV_CONSUMER_ID("Nav-Consumer-Id");

    fun asString(): String {
        return header
    }
}
