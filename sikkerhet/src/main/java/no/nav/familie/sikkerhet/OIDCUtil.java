package no.nav.familie.sikkerhet;

import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Component
public class OIDCUtil {

    private final TokenValidationContextHolder ctxHolder;

    public OIDCUtil(TokenValidationContextHolder ctxHolder) {
        this.ctxHolder = ctxHolder;
    }

    public String getSubject() {
        return Optional.ofNullable(claimSet())
                       .map(JwtTokenClaims::getSubject)
                       .orElse(null);
    }

    public String autentisertBruker() {
        return Optional.ofNullable(getSubject())
                       .orElseThrow(() -> new JwtTokenValidatorException("Fant ikke subject", getExpiryDate()));
    }

    public String getNavIdent() {
        return Optional.ofNullable(claimSet())
                       .map(c -> c.get("NAVident"))
                       .map(Object::toString)
                       .orElseThrow(() -> new JwtTokenValidatorException("Fant ikke NAVident", getExpiryDate()));
    }

    public List<String> getGroups() {
        return Optional.ofNullable(claimSet())
                       .map(c -> c.get("groups"))
                       .stream()
                       .map(Object::toString)
                       .collect(Collectors.toList());

    }

    public JwtTokenClaims claimSet() {
        return Optional.ofNullable(context())
                       .map(s -> s.getClaims("azuread"))
                       .orElse(null);
    }

    private TokenValidationContext context() {
        return Optional.ofNullable(ctxHolder.getTokenValidationContext())
                       .orElse(null);
    }

    public Date getExpiryDate() {
        return Optional.ofNullable(claimSet())
                       .map(c -> c.get("exp"))
                       .map(this::getDateClaim)
                       .orElse(null);
    }

    public Date getDateClaim(Object value) {
        if (value instanceof Date) {
            return Date.class.cast(value);
        }
        if (value instanceof Number) {
            return new Date(Number.class.cast(value).longValue() * 1000L);
        }
        return null;
    }

}
