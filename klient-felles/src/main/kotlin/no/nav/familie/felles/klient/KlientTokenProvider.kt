package no.nav.familie.felles.klient

fun interface KlientTokenProvider {
    fun hentToken(): String?
}
