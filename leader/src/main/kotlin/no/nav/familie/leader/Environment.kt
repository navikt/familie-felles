package no.nav.familie.leader

object Environment {
    @JvmStatic
    fun hentLeaderSystemEnv(): String? {
        return System.getenv("ELECTOR_GET_URL") ?: return null
    }
}
