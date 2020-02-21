package no.nav.familie.log.filter;

import no.nav.familie.log.NavHttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class LogFilterTest {

    private HttpServletRequest httpServletRequest ;
    private HttpServletResponse httpServletResponse ;

    private final LogFilter logFilter = new LogFilter();

    @Before
    public void setup() {
        httpServletRequest = getMockHttpServletRequest();
        httpServletResponse = getMockHttpServletResponse();
    }

    @Test
    public void cleanupOfMDCContext() throws ServletException, IOException {
        Map<String, String> initialContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElseGet(HashMap::new);
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> {
        });
        assertThat(initialContextMap).isEqualTo(Optional.ofNullable(MDC.getCopyOfContextMap()).orElseGet(HashMap::new));
    }

    @Test
    public void addResponseHeaders() throws ServletException, IOException {
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> {
        });

        assertThat(httpServletResponse.getHeader(NavHttpHeaders.NAV_CALL_ID.asString())).isNotEmpty();
        assertThat(httpServletResponse.getHeader("Server")).isNull();
    }

    @Test
    public void handleExceptions() throws ServletException, IOException {
        logFilter.doFilter(httpServletRequest, httpServletResponse, (request, response) -> fail());
        assertThat(httpServletResponse.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
    }

    private static void fail() {
        throw new IllegalStateException("");
    }


    private static HttpServletRequest getMockHttpServletRequest() {
        final String method = "GET";
        final String requestUri = "/test/path";

        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(requestUri);
        return request;
    }

    private static HttpServletResponse getMockHttpServletResponse() {

        var response = mock(HttpServletResponse.class);
        final Map<String, String> headers = new HashMap<>();
        final int[] status = {0};

        Answer<Void> statusAnswer = invocationOnMock -> {
            status[0] = invocationOnMock.getArgument(0);
            return null;
        };
        doAnswer(statusAnswer).when(response).setStatus(anyInt());

        Answer<Void> headerAnswer = invocationOnMock -> {
            headers.put(invocationOnMock.getArgument(0), invocationOnMock.getArgument(1));
            return null;
        };
        doAnswer(headerAnswer).when(response).setHeader(anyString(), anyString());

        when(response.getStatus()).thenAnswer(invocationOnMock -> status[0]);

        doAnswer(invocationOnMock -> {
            String s = invocationOnMock.getArgument(0);
            return headers.get(s);
        }).when(response).getHeader(anyString());

        when(response.getHeaderNames()).thenAnswer(invocationOnMock -> headers.keySet());

        return response;
    }
}
