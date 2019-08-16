package no.nav.familie.log.filter;

import no.nav.familie.log.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

import static no.nav.familie.log.IdUtils.generateId;
import static no.nav.familie.log.mdc.MDCConstants.*;

public class LogFilter implements Filter {

    public static final String CONSUMER_ID_HEADER_NAME = "Nav-Consumer-Id";
    // there is no consensus in NAV about header-names for correlation ids, so we support 'em all!
    // https://nav-it.slack.com/archives/C9UQ16AH4/p1538488785000100
    public static final String PREFERRED_NAV_CALL_ID_HEADER_NAME = "Nav-Call-Id";
    public static final String[] NAV_CALL_ID_HEADER_NAMES = {
            PREFERRED_NAV_CALL_ID_HEADER_NAME,
            "Nav-CallId",
            "X-Correlation-Id"
    };
    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);
    private static final String RANDOM_USER_ID_COOKIE_NAME = "RUIDC";
    private static final int ONE_MONTH_IN_SECONDS = 60 * 60 * 24 * 30;


    /**
     * Filter init param used to specify a {@link Supplier <Boolean>} that will return whether stacktraces should be exposed or not
     * Defaults to always false
     */
    private final Supplier<Boolean> exposeErrorDetails;
    private final String serverName;

    public LogFilter() {
        this(() -> false, null);
    }

    public LogFilter(Supplier<Boolean> exposeErrorDetails, String serverName) {
        this.exposeErrorDetails = exposeErrorDetails;
        this.serverName = serverName;
    }

    public static String resolveCallId(HttpServletRequest httpServletRequest) {
        return Arrays.stream(NAV_CALL_ID_HEADER_NAMES).map(httpServletRequest::getHeader)
                .filter(it -> it != null && !it.isEmpty())
                .findFirst()
                .orElseGet(IdUtils::generateId);
    }

    private void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String userId = resolveUserId(httpServletRequest);
        if (userId == null || userId.isEmpty()) {
            // user-id tracking only works if the client is stateful and supports cookies.
            // if no user-id is found, generate one for any following requests but do not use it on the current request to avoid generating large numbers of useless user-ids.
            generateUserIdCookie(httpServletResponse);
        }

        String consumerId = httpServletRequest.getHeader(CONSUMER_ID_HEADER_NAME);
        String callId = resolveCallId(httpServletRequest);

        MDC.put(MDC_CALL_ID, callId);
        MDC.put(MDC_USER_ID, userId);
        MDC.put(MDC_CONSUMER_ID, consumerId);
        MDC.put(MDC_REQUEST_ID, generateId());

        httpServletResponse.setHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, callId);

        if (serverName != null) {
            httpServletResponse.setHeader("Server", serverName);
        }

        try {
            filterWithErrorHandling(httpServletRequest, httpServletResponse, filterChain);
        } finally {
            MDC.remove(MDC_CALL_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CONSUMER_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private void generateUserIdCookie(HttpServletResponse httpServletResponse) {
        String userId = generateId();
        Cookie cookie = new Cookie(RANDOM_USER_ID_COOKIE_NAME, userId);
        cookie.setPath("/");
        cookie.setMaxAge(ONE_MONTH_IN_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        httpServletResponse.addCookie(cookie);
    }

    private void filterWithErrorHandling(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (httpServletResponse.isCommitted()) {
                log.error("failed with status={}", httpServletResponse.getStatus());
                throw e;
            } else {
                httpServletResponse.setStatus(500);
                if (exposeErrorDetails.get()) {
                    e.printStackTrace(httpServletResponse.getWriter());
                }
            }
        }
    }

    public String resolveUserId(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (RANDOM_USER_ID_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        doFilterInternal((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
    }

    @Override
    public void destroy() {

    }
}
