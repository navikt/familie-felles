package no.nav.familie.sikkerhet.context

import kotlinx.coroutines.ThreadContextElement
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Et [CoroutineContext]-element som fanger opp Spring Webs [RequestAttributes] ved coroutine-oppstart
 * og gjenoppretter dem for hver continuation – uavhengig av hvilken tråd coroutinen kjører på.
 *
 * Nødvendig fordi nav-token-support lagrer token-valideringskonteksten i [RequestContextHolder]
 * (ThreadLocal), som ikke følger med coroutiner på tvers av tråder (f.eks. [kotlinx.coroutines.Dispatchers.IO]).
 *
 * Bruk:
 * ```kotlin
 * launch(Dispatchers.IO + RequestContextCoroutineContext()) {
 *     // Token-kontekst er tilgjengelig her
 * }
 * ```
 */
class RequestContextCoroutineContext(
    private val requestAttributes: RequestAttributes? = RequestContextHolder.getRequestAttributes(),
) : AbstractCoroutineContextElement(Key),
    ThreadContextElement<RequestAttributes?> {
    companion object Key : CoroutineContext.Key<RequestContextCoroutineContext>

    override fun updateThreadContext(context: CoroutineContext): RequestAttributes? {
        val previous = RequestContextHolder.getRequestAttributes()
        if (requestAttributes != null) {
            RequestContextHolder.setRequestAttributes(requestAttributes, false)
        } else {
            RequestContextHolder.resetRequestAttributes()
        }
        return previous
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: RequestAttributes?,
    ) {
        if (oldState != null) {
            RequestContextHolder.setRequestAttributes(oldState, false)
        } else {
            RequestContextHolder.resetRequestAttributes()
        }
    }
}
