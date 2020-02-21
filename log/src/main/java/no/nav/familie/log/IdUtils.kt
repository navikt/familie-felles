package no.nav.familie.log

import java.util.*

object IdUtils {
    @JvmStatic fun generateId(): String {
        val uuid = UUID.randomUUID()
        return java.lang.Long.toHexString(uuid.mostSignificantBits) + java.lang.Long.toHexString(uuid.leastSignificantBits)
    }
}
