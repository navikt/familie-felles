package no.nav.familie.sikkerhet

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class AuthorizationFilter(
    private val oidcUtil: OIDCUtil,
    private val acceptedClients: List<String>,
    private val disabled: Boolean = false,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        when (disabled || acceptedClient()) {
            true -> filterChain.doFilter(request, response)
            false -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authenticated, but unauthorized application")
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.substring(request.contextPath.length)
        return path.startsWith("/api/selvbetjening") ||
            path.startsWith("/internal/") ||
            path.startsWith("/swagger-ui/") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/v2/api-docs")
    }

    private fun acceptedClient() = acceptedClients.contains(oidcUtil.getClaim("azp"))
}
