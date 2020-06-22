package no.nav.familie.leader

object Environment {
    @JvmStatic
    fun hentLeaderSystemEnv(): String? {
        return System.getenv("ELECTOR_PATH") ?: return null
    }
}
