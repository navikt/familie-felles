package no.nav.familie.sikkerhet

import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthorizationFilter(
        private val oidcUtil: OIDCUtil,
        private val acceptedClients: List<AcceptedClient>,
        private val disabled: Boolean = false
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        when (disabled || isAuthorized(request.requestURI)) {
            true -> filterChain.doFilter(request, response)
            false -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authenticated, but unauthorized application")
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.substring(request.contextPath.length)
        return path.startsWith("/api/selvbetjening") || path.startsWith("/internal/")
    }

    private fun isAuthorized(path: String) = acceptedClients.any {
        it.clientId == oidcUtil.getClaim("azp") && it.acceptedPaths.any { pathPrefix -> path.startsWith(pathPrefix) }
    }

}
