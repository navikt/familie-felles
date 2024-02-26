package no.nav.familie.sikkerhet

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Filter for å validere tokens, kan settes til å kun validere on behalf of/client credential eller begge
 *
 * @param acceptClientCredential true/false
 * @param acceptOnBehalfOf true/false
 * @param issuerName default azuread, kan overskreves hvis issuer er eks aad/azure
 * @param logOnly hvis man ønsker å kun logge resultatet men ikke stoppe requesten
 */
class ClientTokenValidationFilter(
    private val acceptClientCredential: Boolean = false,
    private val acceptOnBehalfOf: Boolean = false,
    private val issuerName: String = "azuread",
    private val logOnly: Boolean = false,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val accepted = accepted()
        loggResultat(accepted)
        when (accepted || logOnly) {
            true -> filterChain.doFilter(request, response)
            false -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authenticated, but unauthorized application")
        }
    }

    private fun loggResultat(accepted: Boolean) {
        val melding = "Validerer token accepted=$accepted logOnly=$logOnly"
        if (logOnly) {
            logger.info(melding)
        } else {
            logger.debug(melding)
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.substring(request.contextPath.length)
        return path.startsWith("/internal/") ||
            path.startsWith("/swagger-ui/") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/v2/api-docs") ||
            shouldNotFilter(path)
    }

    /**
     * Kan overrideas hvis man ønsker å filrere bort andre paths
     */
    @Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
    fun shouldNotFilter(path: String): Boolean = false

    private fun accepted(): Boolean {
        try {
            val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims(issuerName)

            if (claims == null) {
                logger.warn("Finner ikke claim=$issuerName")
                return false
            }

            val sub = claims.get("sub") as String?
            val oid = claims.get("oid") as String?
            val clientId = claims.get("azp") as String?

            @Suppress("UNCHECKED_CAST")
            val roles = claims.get("roles") as List<String>? ?: emptyList()
            val accessAsApplication = roles.contains("access_as_application")

            val erClientCredential = sub != null && sub == oid
            if (acceptClientCredential && accessAsApplication) {
                return true
            }
            if (acceptOnBehalfOf && !erClientCredential) {
                return true
            }
            logger.warn(
                "Mangler noe i token - accessAsApplication=$accessAsApplication clientId=$clientId erClientCredential=$erClientCredential",
            )
            return false
        } catch (e: Exception) {
            logger.error("Feilet sjekk av access_as_application", e)
            return false
        }
    }
}
