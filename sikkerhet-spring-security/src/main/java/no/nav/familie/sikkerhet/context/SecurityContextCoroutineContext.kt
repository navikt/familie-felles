package no.nav.familie.sikkerhet.context

import kotlinx.coroutines.ThreadContextElement
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Et [CoroutineContext]-element som fanger opp Spring Securitys [SecurityContext] ved coroutine-oppstart
 * og gjenoppretter den for hver continuation – uavhengig av hvilken tråd coroutinen kjører på.
 *
 * Nødvendig fordi [SecurityContextHolder] bruker [ThreadLocal], som ikke følger med coroutiner
 * på tvers av tråder (f.eks. [kotlinx.coroutines.Dispatchers.IO]).
 *
 * Bruk:
 * ```kotlin
 * launch(Dispatchers.IO + SecurityContextCoroutineContext()) {
 *     // SecurityContext er tilgjengelig her
 * }
 * ```
 */
class SecurityContextCoroutineContext(
    private val securityContext: SecurityContext = SecurityContextHolder.getContext(),
) : AbstractCoroutineContextElement(Key),
    ThreadContextElement<SecurityContext?> {
    companion object Key : CoroutineContext.Key<SecurityContextCoroutineContext>

    override fun updateThreadContext(context: CoroutineContext): SecurityContext? {
        val previous = SecurityContextHolder.getContext()
        SecurityContextHolder.setContext(securityContext)
        return previous
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: SecurityContext?,
    ) {
        if (oldState != null) {
            SecurityContextHolder.setContext(oldState)
        } else {
            SecurityContextHolder.clearContext()
        }
    }
}
